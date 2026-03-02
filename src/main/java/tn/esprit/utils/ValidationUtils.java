package tn.esprit.utils;

/**
 * ValidationUtils — all field validation rules in one place.
 * Used by controllers and services.  No JavaFX imports here so it
 * can be unit-tested without a running JavaFX runtime.
 */
public class ValidationUtils {

    private ValidationUtils() {}   // utility class — no instances

    // ── email ─────────────────────────────────────────────────────────

    public static boolean isValidEmail(String email) {
        if (email == null || email.isBlank()) return false;
        return email.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");
    }

    // ── password ──────────────────────────────────────────────────────

    /** At least 6 characters. */
    public static boolean isValidPassword(String password) {
        return password != null && password.length() >= 6;
    }

    /**
     * Returns a human-readable reason why the password is invalid,
     * or null if it is valid.  Useful for showing specific error messages.
     */
    public static String passwordError(String password) {
        if (password == null || password.isBlank()) return "Password is required.";
        if (password.length() < 6)                  return "Password must be at least 6 characters.";
        return null;
    }

    // ── phone ─────────────────────────────────────────────────────────

    /** Tunisian phone: exactly 8 digits. */
    public static boolean isValidPhone(String phone) {
        return phone != null && phone.matches("\\d{8}");
    }

    // ── name ──────────────────────────────────────────────────────────

    /** Letters and spaces only, at least 2 characters. */
    public static boolean isValidName(String name) {
        return name != null && name.matches("[a-zA-Z ]{2,}");
    }
}