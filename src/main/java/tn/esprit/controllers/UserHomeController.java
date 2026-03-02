package tn.esprit.controllers;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.stage.Stage;
import tn.esprit.entities.Role;
import tn.esprit.entities.User;
import tn.esprit.services.UserService;
import tn.esprit.utils.SessionManager;

import java.io.IOException;
import java.time.format.DateTimeFormatter;

public class UserHomeController {

    @FXML private Label welcomeLabel;
    @FXML private Label profileName;
    @FXML private Label profileEmail;
    @FXML private Label profilePhone;
    @FXML private Label profileRole;
    @FXML private Label profileVerified;
    @FXML private Label profileCreatedAt;

    private final UserService userService = new UserService();

    /**
     * Called by LoginController right after loading this FXML.
     * Also stores the user in SessionManager so other screens can access it.
     */
    public void setUser(User user) {
        SessionManager.setCurrentUser(user);
        populateView(user);
    }

    @FXML
    public void initialize() {
        // If already in session (e.g. navigated back from another screen)
        if (SessionManager.isLoggedIn()) {
            populateView(SessionManager.getCurrentUser());
        }
    }

    private void populateView(User user) {
        welcomeLabel.setText("Welcome, " + user.getName() + "!");
        profileName.setText(user.getName());
        profileEmail.setText(user.getEmail());
        profilePhone.setText(user.getPhone());

        Role role = userService.getRoleById(user.getRoleId());
        profileRole.setText(role != null ? role.getRoleName() : "Role #" + user.getRoleId());

        if (user.isVerified()) {
            profileVerified.setText("✅ Verified");
            profileVerified.setStyle("-fx-text-fill: #16A34A; -fx-font-size: 14px; -fx-font-weight: bold;");
        } else {
            profileVerified.setText("⏳ Not verified");
            profileVerified.setStyle("-fx-text-fill: #D97706; -fx-font-size: 14px;");
        }

        if (user.getCreatedAt() != null) {
            DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd MMM yyyy");
            profileCreatedAt.setText(user.getCreatedAt().format(fmt));
        } else {
            profileCreatedAt.setText("—");
        }
    }

    @FXML
    private void handleLogout() {
        SessionManager.logout();
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/Login.fxml"));
            Stage stage = (Stage) welcomeLabel.getScene().getWindow();
            stage.setScene(new Scene(root, 900, 600));
            stage.setTitle("FinTech Portal – Login");
            stage.centerOnScreen();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}