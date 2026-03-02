package tn.esprit.services;

import tn.esprit.dao.RoleDAO;
import tn.esprit.dao.UserDAO;
import tn.esprit.entities.Role;
import tn.esprit.entities.User;

import java.util.List;

/**
 * UserService — business logic for user data management.
 *
 * Responsibilities:
 *  - Admin CRUD (create user with temp password, update info, delete)
 *  - Profile updates for the logged-in user
 *  - Role lookups
 *
 * Does NOT handle: login, registration, password hashing, emails.
 * Those live in AuthService.
 */
public class UserService {

    private final UserDAO userDAO;
    private final RoleDAO roleDAO;

    public UserService() {
        this.userDAO = new UserDAO();
        this.roleDAO = new RoleDAO();
    }

    // ── read ──────────────────────────────────────────────────────────

    public List<User> getAllUsers() {
        return userDAO.readAll();
    }

    public User getUserById(int id) {
        return userDAO.read(id);
    }

    public User getUserByEmail(String email) {
        return userDAO.findByEmail(email);
    }

    // ── admin create ──────────────────────────────────────────────────

    /**
     * Admin creates a new user account.
     * The password is already hashed by AuthService before calling this.
     * The account is marked verified=true because the admin created it manually.
     */
    public boolean adminCreateUser(User user) {
        user.setVerified(true);         // admin-created accounts skip email verification
        user.setVerificationCode(null);
        return userDAO.create(user);
    }

    // ── admin update ──────────────────────────────────────────────────

    /**
     * Admin updates a user's profile fields.
     * Password is intentionally excluded — admins cannot change passwords.
     */
    public boolean adminUpdateUser(User user) {
        return userDAO.update(user);
    }

    // ── admin delete ──────────────────────────────────────────────────

    public boolean deleteUser(int id) {
        return userDAO.delete(id);
    }

    // ── profile self-edit ─────────────────────────────────────────────

    /**
     * Logged-in user updates their own name and phone.
     * Email changes are intentionally not allowed here to avoid
     * breaking the login flow — that would need re-verification.
     */
    public boolean updateProfile(User user) {
        return userDAO.update(user);
    }

    // ── roles ─────────────────────────────────────────────────────────

    public List<Role> getAllRoles() {
        return roleDAO.readAll();
    }

    public Role getRoleById(int id) {
        return roleDAO.read(id);
    }

    // ── helpers ───────────────────────────────────────────────────────

    public boolean emailExists(String email) {
        return userDAO.emailExists(email);
    }
}