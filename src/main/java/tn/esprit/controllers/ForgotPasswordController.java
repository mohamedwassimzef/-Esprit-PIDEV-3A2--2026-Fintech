package tn.esprit.controllers;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;
import tn.esprit.dao.UserDAO;
import tn.esprit.entities.User;
import tn.esprit.services.AuthService;
import tn.esprit.services.EmailService;

import java.io.IOException;

/**
 * Step 1 of the forgot-password flow.
 * User enters their email → we send a reset code → navigate to ResetPasswordController.
 */
public class ForgotPasswordController {

    @FXML private TextField emailField;
    @FXML private Label     messageLabel;
    @FXML private Button    sendBtn;

    private final UserDAO     userDAO     = new UserDAO();
    private final AuthService authService = new AuthService();

    @FXML
    private void handleSend() {
        String email = emailField.getText().trim().toLowerCase();

        if (email.isEmpty()) {
            showMessage("Please enter your email address.", "red");
            return;
        }

        User user = userDAO.findByEmail(email);
        if (user == null) {
            // Don't reveal whether the email exists — security best practice
            showMessage("If that email is registered, a reset code has been sent.", "#1E3A8A");
            return;
        }

        // Generate code, save to DB, send email
        String code = authService.resendVerificationCode(email); // reuses same code storage
        EmailService.sendPasswordResetEmail(email, user.getName(), code);

        showMessage("📧 Reset code sent! Check your inbox.", "#16A34A");
        sendBtn.setDisable(true);

        // Navigate to reset screen after brief pause
        new Thread(() -> {
            try { Thread.sleep(1200); } catch (InterruptedException ignored) {}
            javafx.application.Platform.runLater(() -> goToReset(email, user.getName()));
        }).start();
    }

    private void goToReset(String email, String name) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/ResetPassword.fxml"));
            Parent root = loader.load();
            ResetPasswordController ctrl = loader.getController();
            ctrl.init(email, name);
            Stage stage = (Stage) emailField.getScene().getWindow();
            stage.setScene(new Scene(root, 900, 600));
            stage.setTitle("FinTech Portal – Reset Password");
            stage.centerOnScreen();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void handleBackToLogin() {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/Login.fxml"));
            Stage stage = (Stage) emailField.getScene().getWindow();
            stage.setScene(new Scene(root, 900, 600));
            stage.setTitle("FinTech Portal – Login");
            stage.centerOnScreen();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void showMessage(String text, String color) {
        messageLabel.setText(text);
        messageLabel.setStyle("-fx-text-fill: " + color + "; -fx-font-weight: bold; -fx-font-size: 13px;");
    }
}