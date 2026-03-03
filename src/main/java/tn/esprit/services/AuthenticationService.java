package tn.esprit.services;

import tn.esprit.dao.UserDAO;
import tn.esprit.entities.User;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

/**
 * Authentication service — login, register, verify, password reset.
 */
public class AuthenticationService {

    private final UserDAO userDAO;
    private static final SecureRandom RANDOM = new SecureRandom();

    public AuthenticationService() {
        this.userDAO = new UserDAO();
    }

    // ── Login ──────────────────────────────────────────────────────────────

    public User authenticate(String email, String password) {
        if (email == null || email.isBlank() || password == null || password.isBlank()) return null;

        User user = userDAO.findByEmail(email);
        if (user == null) return null;

        String inputHash = hashPassword(password);
        boolean match = (inputHash != null && inputHash.equals(user.getPasswordHash()))
                     || password.equals(user.getPasswordHash()); // dev fallback

        if (match) {
            userDAO.updateLastLogin(user.getId());
            // Send login notification asynchronously
            EmailService.sendLoginNotification(user.getEmail(), user.getName());
            return user;
        }
        return null;
    }

    // ── Register ───────────────────────────────────────────────────────────

    /**
     * Creates the account with is_verified=false and a 6-digit code,
     * then fires the verification email.
     * @return the new (unverified) User, or null on failure.
     */
    public User register(String name, String email, String password, String phone) {
        if (name == null || name.isBlank() || email == null || email.isBlank()
                || password == null || password.isBlank()) return null;

        if (userDAO.findByEmail(email) != null) return null;

        String hashed = hashPassword(password);
        if (hashed == null) return null;

        String code = generateCode();

        User newUser = new User(name.trim(), email.trim().toLowerCase(),
                hashed, 2, phone == null ? "" : phone.trim());
        newUser.setVerified(false);
        newUser.setVerificationCode(code);

        if (!userDAO.create(newUser)) return null;

        // Reload to get the generated id
        User saved = userDAO.findByEmail(email.trim().toLowerCase());
        if (saved == null) return null;

        // Send verification email (runs on background thread inside EmailService)
        EmailService.sendVerificationEmail(saved.getEmail(), saved.getName(), code);
        return saved;
    }

    // ── Email verification ─────────────────────────────────────────────────

    /**
     * Verify the 6-digit code the user received by email.
     * @return true if code matched and account is now marked verified.
     */
    public boolean verifyEmail(int userId, String code) {
        User user = userDAO.read(userId);
        if (user == null || code == null) return false;
        if (!code.trim().equals(user.getVerificationCode())) return false;
        return userDAO.markVerified(userId);
    }

    /**
     * Resend a fresh verification code to the user.
     */
    public boolean resendVerificationCode(int userId) {
        User user = userDAO.read(userId);
        if (user == null) return false;
        String code = generateCode();
        user.setVerificationCode(code);
        if (!userDAO.update(user)) return false;
        EmailService.sendVerificationEmail(user.getEmail(), user.getName(), code);
        return true;
    }

    // ── Forgot / Reset password ────────────────────────────────────────────

    /**
     * Sends a password-reset code to the given email.
     * @return the user id if the email exists, -1 otherwise.
     */
    public int sendPasswordResetCode(String email) {
        User user = userDAO.findByEmail(email.trim().toLowerCase());
        if (user == null) return -1;
        String code = generateCode();
        user.setVerificationCode(code);
        if (!userDAO.update(user)) return -1;
        EmailService.sendPasswordResetEmail(user.getEmail(), user.getName(), code);
        return user.getId();
    }

    /**
     * Validates the reset code and sets the new password.
     * @return true on success.
     */
    public boolean resetPassword(int userId, String code, String newPassword) {
        User user = userDAO.read(userId);
        if (user == null || code == null || newPassword == null) return false;
        if (!code.trim().equals(user.getVerificationCode())) return false;
        String hashed = hashPassword(newPassword);
        if (hashed == null) return false;
        user.setPasswordHash(hashed);
        user.setVerificationCode(null);
        return userDAO.update(user);
    }

    // ── Helpers ────────────────────────────────────────────────────────────

    public boolean isAdmin(User user) {
        return user != null && user.getRoleId() == 1;
    }

    /** Generates a 6-digit numeric code as a String. */
    public static String generateCode() {
        return String.format("%06d", RANDOM.nextInt(1_000_000));
    }

    public static String hashPassword(String password) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(password.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) sb.append('0');
                sb.append(hex);
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            System.out.println("Error hashing password: " + e.getMessage());
            return null;
        }
    }
}
