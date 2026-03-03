package tn.esprit.controllers;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;
import tn.esprit.dao.UserDAO;
import tn.esprit.entities.User;
import tn.esprit.services.EmailService;
import tn.esprit.services.FaceRecognitionService;
import tn.esprit.utils.SessionManager;

import java.io.IOException;

/**
 * FaceLoginController
 * User types their email, we load their LBPH model, then open the webcam to verify.
 */
public class FaceLoginController {

    @FXML private TextField emailField;
    @FXML private Label     statusLabel;
    @FXML private Button    scanBtn;

    private final FaceRecognitionService faceService = new FaceRecognitionService();
    private final UserDAO                userDAO     = new UserDAO();

    @FXML
    private void handleScanFace() {
        String email = emailField.getText().trim().toLowerCase();
        if (email.isEmpty()) {
            setStatus("\u26a0 Please enter your email first.", "red"); return;
        }
        User user = userDAO.findByEmail(email);
        if (user == null) {
            setStatus("\u274c No account found with that email.", "red"); return;
        }
        if (!user.isFaceRegistered()) {
            setStatus("\u26a0 Face not enrolled yet. Log in and register your face in your profile.", "orange");
            return;
        }

        scanBtn.setDisable(true);
        setStatus("\uD83D\uDCF7 Opening webcam \u2014 look at the camera...", "#1E3A8A");

        int userId = user.getId();
        new Thread(() -> {
            boolean ok = faceService.recognise(userId);
            Platform.runLater(() -> {
                if (ok) {
                    setStatus("\u2705 Face recognised! Logging you in...", "#16A34A");
                    SessionManager.setCurrentUser(user);
                    EmailService.sendLoginNotification(user.getEmail(), user.getName());
                    new Thread(() -> {
                        try { Thread.sleep(800); } catch (InterruptedException ignored) {}
                        Platform.runLater(() -> navigateAfterLogin(user));
                    }).start();
                } else {
                    setStatus("\u274c Face not recognised. Try again or use another login method.", "red");
                    scanBtn.setDisable(false);
                }
            });
        }).start();
    }

    @FXML
    private void handleBackToLogin() {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/Login.fxml"));
            Stage stage = (Stage) emailField.getScene().getWindow();
            stage.setScene(new Scene(root, 900, 600));
            stage.setTitle("FinTech Portal \u2013 Login");
            stage.centerOnScreen();
        } catch (IOException e) { e.printStackTrace(); }
    }

    private void navigateAfterLogin(User user) {
        String fxml = SessionManager.isAdmin() ? "UserManagement.fxml" : "UserHome.fxml";
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/" + fxml));
            Parent root = loader.load();
            if (!SessionManager.isAdmin()) {
                ((UserHomeController) loader.getController()).setUser(user);
            }
            Stage stage = (Stage) emailField.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle("FinTech Portal");
            stage.centerOnScreen();
        } catch (IOException e) { e.printStackTrace(); }
    }

    private void setStatus(String text, String color) {
        statusLabel.setText(text);
        statusLabel.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: " + color + ";");
    }
}
