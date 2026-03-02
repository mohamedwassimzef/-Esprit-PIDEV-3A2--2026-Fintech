package tn.esprit.services;

import tn.esprit.dao.UserDAO;
import tn.esprit.entities.User;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Authentication service that verifies user credentials.
 * Uses SHA-256 hashing to compare passwords.
 */
public class AuthenticationService {

    private final UserDAO userDAO;

    public AuthenticationService() {
        this.userDAO = new UserDAO();
    }

    /**
     * Authenticate a user by email and password.
     * @param email    user email
     * @param password plain-text password
     * @return the authenticated User, or null if credentials are invalid
     */
    public User authenticate(String email, String password) {
        if (email == null || email.isBlank() || password == null || password.isBlank()) {
            return null;
        }

        User user = userDAO.findByEmail(email);
        if (user == null) {
            return null;
        }

        // Compare password hash
        String inputHash = hashPassword(password);
        if (inputHash != null && inputHash.equals(user.getPasswordHash())) {
            return user;
        }

        // Fallback: also allow plain-text comparison for dev/testing
        // (remove this in production)
        if (password.equals(user.getPasswordHash())) {
            return user;
        }

        return null;
    }

    /**
     * Hash a password using SHA-256.
     */
    public static String hashPassword(String password) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(password.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            System.out.println("Error hashing password: " + e.getMessage());
            return null;
        }
    }

    /**
     * Register a new user with role=2 (regular user).
     * @param name     full name
     * @param email    email address
     * @param password plain-text password
     * @param phone    phone number (may be empty)
     * @return true if registration succeeded, false if email already exists or DB error
     */
    public boolean register(String name, String email, String password, String phone) {
        if (name == null || name.isBlank() || email == null || email.isBlank()
                || password == null || password.isBlank()) {
            return false;
        }
        // Check email is not already taken
        if (userDAO.findByEmail(email) != null) {
            return false;
        }
        String hashed = hashPassword(password);
        if (hashed == null) return false;

        User newUser = new User(name.trim(), email.trim().toLowerCase(),
                hashed, 2, phone == null ? "" : phone.trim());
        return userDAO.create(newUser);
    }

    /**
     * Check if a user has admin role (roleId == 1).
     */
    public boolean isAdmin(User user) {
        return user != null && user.getRoleId() == 1;
    }
}

