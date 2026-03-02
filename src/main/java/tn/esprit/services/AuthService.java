package tn.esprit.services;

import org.mindrot.jbcrypt.BCrypt;
import tn.esprit.dao.UserDAO;
import tn.esprit.entities.User;
import tn.esprit.utils.ValidationUtils;

import java.util.Random;

/**
 * AuthService — all authentication and password logic.
 *
 * Responsibilities:
 *  - Hashing passwords with BCrypt (one place, nowhere else)
 *  - Login: fetch user by email, verify BCrypt hash
 *  - Register: validate, hash password, generate verification code, persist
 *  - Change password: verify old password, hash new one, persist
 *  - Reset password: used after forgot-password code is verified
 *  - Generate temporary passwords for admin-created accounts
 */
public class AuthService {

    private final UserDAO userDAO;

    public AuthService() {
        this.userDAO = new UserDAO();
    }

    // ── login ─────────────────────────────────────────────────────────

    /**
     * Attempts to log in with email + plain password.
     * Returns a LoginResult describing the outcome.
     */
    public LoginResult login(String email, String plainPassword) {
        if (email == null || email.isBlank() || plainPassword == null || plainPassword.isBlank())
            return LoginResult.INVALID_INPUT;

        User user = userDAO.findByEmail(email.trim().toLowerCase());
        if (user == null)
            return LoginResult.USER_NOT_FOUND;

        if (!BCrypt.checkpw(plainPassword, user.getPasswordHash()))
            return LoginResult.WRONG_PASSWORD;

        if (!user.isVerified())
            return LoginResult.NOT_VERIFIED;

        userDAO.updateLastLogin(user.getId());

        return LoginResult.success(user);
    }

    // ── register ──────────────────────────────────────────────────────

    /**
     * Validates input, hashes the password, generates a verification code,
     * and persists the new user.
     * The caller (controller) is responsible for sending the verification email.
     */
    public RegisterResult register(String name, String email,
                                   String plainPassword, String phone, int roleId) {
        if (!ValidationUtils.isValidName(name))
            return RegisterResult.error("Name must contain only letters and be at least 2 characters.");
        if (!ValidationUtils.isValidEmail(email))
            return RegisterResult.error("Invalid email format.");
        if (!ValidationUtils.isValidPassword(plainPassword))
            return RegisterResult.error(ValidationUtils.passwordError(plainPassword));
        if (!ValidationUtils.isValidPhone(phone))
            return RegisterResult.error("Phone must be exactly 8 digits.");

        if (userDAO.emailExists(email.trim().toLowerCase()))
            return RegisterResult.error("This email is already registered.");

        String hashed = BCrypt.hashpw(plainPassword, BCrypt.gensalt());
        String code   = generateVerificationCode();

        User user = new User(name.trim(), email.trim().toLowerCase(),
                hashed, roleId, phone.trim());
        user.setVerificationCode(code);
        user.setVerified(false);

        if (!userDAO.create(user))
            return RegisterResult.error("Registration failed. Please try again.");

        User created = userDAO.findByEmail(email.trim().toLowerCase());
        return RegisterResult.success(created, code);
    }

    // ── change password (requires old password) ───────────────────────

    /**
     * Used by the profile page: verifies the old password first,
     * then updates to the new hashed password.
     */
    public boolean changePassword(User user, String oldPlain, String newPlain) {
        if (!BCrypt.checkpw(oldPlain, user.getPasswordHash())) return false;
        if (!ValidationUtils.isValidPassword(newPlain))        return false;

        String newHashed = BCrypt.hashpw(newPlain, BCrypt.gensalt());
        return userDAO.updatePassword(user.getId(), newHashed);
    }

    // ── reset password (after forgot-password code verified) ──────────

    /**
     * Directly resets a password without requiring the old one.
     * Only called after the reset code has already been verified.
     */
    public boolean resetPassword(int userId, String newPlain) {
        if (!ValidationUtils.isValidPassword(newPlain)) return false;
        String hashed = BCrypt.hashpw(newPlain, BCrypt.gensalt());
        return userDAO.updatePassword(userId, hashed);
    }

    // ── admin temp password ───────────────────────────────────────────

    /**
     * Generates a random 10-char temporary password, hashes it, stores
     * the hash in the user object, and returns the plain version so the
     * admin can communicate it to the new user once.
     */
    public String generateAndHashTempPassword(User user) {
        String temp   = generateTempPassword();
        String hashed = BCrypt.hashpw(temp, BCrypt.gensalt());
        user.setPasswordHash(hashed);
        return temp;
    }

    // ── verification ──────────────────────────────────────────────────

    /**
     * Checks the code the user typed against the one stored in the DB.
     * Marks the account as verified if correct.
     */
    public boolean verifyAccount(String email, String code) {
        return userDAO.verifyUser(email.trim().toLowerCase(), code.trim());
    }

    /**
     * Generates a new 6-digit code, saves it to the DB, and returns it
     * so the caller can email it. Used for both "resend verification" and
     * "forgot password" flows.
     */
    public String resendVerificationCode(String email) {
        String code = generateVerificationCode();
        userDAO.saveVerificationCode(email.trim().toLowerCase(), code);
        return code;
    }

    // ── lookup ────────────────────────────────────────────────────────

    /** Looks up a user by email — used by ForgotPasswordController. */
    public User findUserByEmail(String email) {
        return userDAO.findByEmail(email.trim().toLowerCase());
    }

    // ── helpers ───────────────────────────────────────────────────────

    private String generateVerificationCode() {
        return String.valueOf(100000 + new Random().nextInt(900000));
    }

    private String generateTempPassword() {
        String chars = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghjkmnpqrstuvwxyz23456789";
        StringBuilder sb = new StringBuilder(10);
        Random rand = new Random();
        for (int i = 0; i < 10; i++) sb.append(chars.charAt(rand.nextInt(chars.length())));
        return sb.toString();
    }

    // ── result types ──────────────────────────────────────────────────

    public static class LoginResult {
        public enum Status { SUCCESS, INVALID_INPUT, USER_NOT_FOUND, WRONG_PASSWORD, NOT_VERIFIED }

        public final Status status;
        public final User   user;

        private LoginResult(Status status, User user) {
            this.status = status;
            this.user   = user;
        }

        public static LoginResult success(User u)       { return new LoginResult(Status.SUCCESS, u); }
        public static final LoginResult INVALID_INPUT   = new LoginResult(Status.INVALID_INPUT,  null);
        public static final LoginResult USER_NOT_FOUND  = new LoginResult(Status.USER_NOT_FOUND, null);
        public static final LoginResult WRONG_PASSWORD  = new LoginResult(Status.WRONG_PASSWORD, null);
        public static final LoginResult NOT_VERIFIED    = new LoginResult(Status.NOT_VERIFIED,   null);

        public boolean isSuccess() { return status == Status.SUCCESS; }
    }

    public static class RegisterResult {
        public final boolean success;
        public final String  errorMessage;
        public final User    user;
        public final String  verificationCode;

        private RegisterResult(boolean success, String errorMessage, User user, String code) {
            this.success          = success;
            this.errorMessage     = errorMessage;
            this.user             = user;
            this.verificationCode = code;
        }

        public static RegisterResult success(User u, String code) {
            return new RegisterResult(true, null, u, code);
        }

        public static RegisterResult error(String msg) {
            return new RegisterResult(false, msg, null, null);
        }
    }
}