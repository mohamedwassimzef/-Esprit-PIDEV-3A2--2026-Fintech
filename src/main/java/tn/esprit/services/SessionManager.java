package tn.esprit.services;

import tn.esprit.entities.User;

/**
 * Singleton that holds the currently logged-in user.
 * All controllers read from this to determine the current user context.
 */
public class SessionManager {

    private static SessionManager instance;
    private User currentUser;

    private SessionManager() {}

    public static SessionManager getInstance() {
        if (instance == null) {
            instance = new SessionManager();
        }
        return instance;
    }

    public void login(User user) {
        this.currentUser = user;
    }

    public void logout() {
        this.currentUser = null;
    }

    public User getCurrentUser() {
        return currentUser;
    }

    public boolean isLoggedIn() {
        return currentUser != null;
    }

    /**
     * Check if the current user has admin role.
     * Convention: roleId == 1 is admin.
     */
    public boolean isAdmin() {
        return currentUser != null && currentUser.getRoleId() == 1;
    }

    public int getUserId() {
        return currentUser != null ? currentUser.getId() : -1;
    }
}

