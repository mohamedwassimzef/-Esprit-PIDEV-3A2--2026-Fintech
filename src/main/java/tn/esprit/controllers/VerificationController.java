package tn.esprit.controllers;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.KeyEvent;
import javafx.stage.Stage;
import tn.esprit.services.AuthService;
import tn.esprit.services.EmailService;

import java.io.IOException;

/**
 * Shown right after registration.
 * The user types the 6-digit code they received by email.
 */
public class VerificationController {

    @FXML private Label     emailLabel;
    @FXML private TextField codeField;
    @FXML private Label     messageLabel;
    @FXML private Button    verifyBtn;
    @FXML private Button    resendBtn;

    private String userEmail;
    private String userName;

    private final AuthService authService = new AuthService();

    /** Called by LoginController right after loading this FXML. */
    public void init(String email, String name) {
        this.userEmail = email;
        this.userName  = name;
        emailLabel.setText("We sent a 6-digit code to " + email);
    }

    @FXML
    public void initialize() {
        // digits only, max 6
        codeField.addEventFilter(KeyEvent.KEY_TYPED, e -> {
            if (!e.getCharacter().matches("[0-9]")) { e.consume(); return; }
            if (codeField.getText().length() >= 6)    e.consume();
        });
    }

    @FXML
    private void handleVerify() {
        String code = codeField.getText().trim();
        if (code.length() != 6) {
            showMessage("Please enter the full 6-digit code.", "red");
            return;
        }

        if (authService.verifyAccount(userEmail, code)) {
            showMessage("✅ Account verified! Redirecting to login...", "#16A34A");
            verifyBtn.setDisable(true);
            resendBtn.setDisable(true);
            // brief pause so the user sees the success message
            new Thread(() -> {
                try { Thread.sleep(1500); } catch (InterruptedException ignored) {}
                Platform.runLater(this::goToLogin);
            }).start();
        } else {
            showMessage("❌ Invalid or expired code. Try again or resend.", "red");
            codeField.clear();
        }
    }

    @FXML
    private void handleResend() {
        String newCode = authService.resendVerificationCode(userEmail);
        EmailService.sendVerificationEmail(userEmail, userName, newCode);
        showMessage("📧 A new code was sent to " + userEmail, "#1E3A8A");
        codeField.clear();
    }

    @FXML
    private void handleBackToLogin() {
        goToLogin();
    }

    private void goToLogin() {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/Login.fxml"));
            Stage stage = (Stage) codeField.getScene().getWindow();
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