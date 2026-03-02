package tn.esprit.utils;

import tn.esprit.entities.User;

/**
 * SessionManager — single source of truth for the logged-in user.
 *
 * Usage:
 *   SessionManager.setCurrentUser(user);   // after successful login
 *   SessionManager.getCurrentUser();       // anywhere in the app
 *   SessionManager.logout();               // on logout
 *   SessionManager.isLoggedIn();           // guard checks
 *   SessionManager.isAdmin();              // role check (role_id == 1)
 */
public class SessionManager {

    private static User currentUser;

    private SessionManager() {}

    public static void setCurrentUser(User user) {
        currentUser = user;
    }

    public static User getCurrentUser() {
        return currentUser;
    }

    public static boolean isLoggedIn() {
        return currentUser != null;
    }

    /** role_id 1 = admin, as defined in your role table. */
    public static boolean isAdmin() {
        return currentUser != null && currentUser.getRoleId() == 1;
    }

    public static void logout() {
        currentUser = null;
    }
}