package controller;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import tn.esprit.dao.UserDAO;
import tn.esprit.entities.User;
import tn.esprit.services.AuthenticationService;
import tn.esprit.services.SessionManager;

/**
 * Controller for VerifyEmail.fxml
 *
 * Shown after registration. The user enters the 6-digit code sent to their email.
 * Call init(userId, email, name) after loading the FXML.
 */
public class VerifyEmailController {

    @FXML private Label     infoLabel;
    @FXML private TextField codeField;
    @FXML private Label     errorLabel;

    private final AuthenticationService authService = new AuthenticationService();

    private int    pendingUserId;
    private String pendingEmail;

    /** Called by LoginController right after loading this scene. */
    public void init(int userId, String email, String name) {
        this.pendingUserId = userId;
        this.pendingEmail  = email;
        infoLabel.setText("A 6-digit verification code was sent to:\n" + email);
    }

    @FXML
    private void handleVerify() {
        String code = codeField.getText().trim();
        if (code.isEmpty()) { showError("Please enter the 6-digit code."); return; }

        boolean ok = authService.verifyEmail(pendingUserId, code);
        if (!ok) { showError("Invalid or expired code. Please try again."); return; }

        User user = new UserDAO().read(pendingUserId);
        if (user != null) {
            SessionManager.getInstance().login(user);
            SceneManager.switchScene("/View/Home.fxml", "FinTech - Home");
        } else {
            SceneManager.switchScene("/View/Login.fxml", "Login");
        }
    }

    @FXML
    private void handleResend() {
        errorLabel.setStyle("-fx-text-fill:#f5c800;-fx-font-size:11px;");
        errorLabel.setText("Sending a new code...");
        boolean ok = authService.resendVerificationCode(pendingUserId);
        if (ok) {
            errorLabel.setText("A new code was sent to " + pendingEmail);
            codeField.clear();
        } else {
            showError("Could not resend the code. Please try again.");
        }
    }

    @FXML
    private void handleBackToLogin() {
        SceneManager.switchScene("/View/Login.fxml", "Login");
    }

    private void showError(String msg) {
        errorLabel.setStyle("-fx-text-fill:#cc2200;-fx-font-size:11px;");
        errorLabel.setText(msg);
    }
}

