package controller;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import tn.esprit.services.AuthenticationService;

/**
 * Controller for ForgotPassword.fxml
 *
 * Two-step flow:
 *   Step 1 - user enters email  -> code sent -> stepResetBox shown
 *   Step 2 - user enters code + new password -> password updated
 */
public class ForgotPasswordController {

    @FXML private Label         titleLabel;
    @FXML private Label         infoLabel;
    @FXML private Label         errorLabel;

    // Step 1
    @FXML private VBox          stepEmailBox;
    @FXML private TextField     emailField;

    // Step 2
    @FXML private VBox          stepResetBox;
    @FXML private TextField     codeField;
    @FXML private PasswordField newPasswordField;
    @FXML private PasswordField confirmPasswordField;

    private final AuthenticationService authService = new AuthenticationService();
    private int pendingUserId = -1;

    // ── Step 1: send code ──────────────────────────────────────────────────

    @FXML
    private void handleSendCode() {
        String email = emailField.getText().trim();
        if (email.isEmpty()) { showError("Please enter your email."); return; }

        showError("");
        infoLabel.setText("Sending code...");

        // run on background thread so UI is not frozen
        new Thread(() -> {
            int uid = authService.sendPasswordResetCode(email);
            javafx.application.Platform.runLater(() -> {
                if (uid == -1) {
                    infoLabel.setText("Enter your email and we'll send you a reset code.");
                    showError("No account found with that email.");
                } else {
                    pendingUserId = uid;
                    // Switch to step 2
                    stepEmailBox.setVisible(false);
                    stepEmailBox.setManaged(false);
                    stepResetBox.setVisible(true);
                    stepResetBox.setManaged(true);
                    titleLabel.setText("RESET PASSWORD");
                    infoLabel.setText("A 6-digit code was sent to:\n" + email);
                }
            });
        }).start();
    }

    // ── Step 2: reset password ─────────────────────────────────────────────

    @FXML
    private void handleReset() {
        String code     = codeField.getText().trim();
        String newPwd   = newPasswordField.getText();
        String confirm  = confirmPasswordField.getText();

        if (code.isEmpty())   { showError("Please enter the reset code."); return; }
        if (newPwd.isEmpty())  { showError("Please enter a new password."); return; }
        if (newPwd.length() < 6) { showError("Password must be at least 6 characters."); return; }
        if (!newPwd.equals(confirm)) { showError("Passwords do not match."); return; }

        boolean ok = authService.resetPassword(pendingUserId, code, newPwd);
        if (!ok) {
            showError("Invalid or expired code. Please try again.");
        } else {
            // Success: go back to login with a message
            errorLabel.setStyle("-fx-text-fill:#22cc66;-fx-font-size:11px;");
            errorLabel.setText("Password reset successful! Redirecting to login...");
            new javafx.animation.Timeline(
                new javafx.animation.KeyFrame(javafx.util.Duration.seconds(1.5),
                    e -> SceneManager.switchScene("/View/Login.fxml", "Login"))
            ).play();
        }
    }

    @FXML
    private void handleBack() {
        SceneManager.switchScene("/View/Login.fxml", "Login");
    }

    private void showError(String msg) {
        errorLabel.setStyle("-fx-text-fill:#cc2200;-fx-font-size:11px;");
        errorLabel.setText(msg);
    }
}

