package tn.esprit.controllers;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.KeyEvent;
import javafx.stage.Stage;
import tn.esprit.dao.UserDAO;
import tn.esprit.entities.User;
import tn.esprit.services.AuthService;
import tn.esprit.utils.ValidationUtils;

import java.io.IOException;

/**
 * Step 2 of the forgot-password flow.
 * User enters the reset code + new password → account password is updated.
 */
public class ResetPasswordController {

    @FXML private Label         infoLabel;
    @FXML private TextField     codeField;
    @FXML private PasswordField newPasswordField;
    @FXML private TextField     newPasswordVisible;
    @FXML private Button        toggleBtn;
    @FXML private PasswordField confirmPasswordField;
    @FXML private Label         messageLabel;
    @FXML private Button        resetBtn;

    private String userEmail;
    private String userName;
    private boolean passVisible = false;

    private final UserDAO     userDAO     = new UserDAO();
    private final AuthService authService = new AuthService();

    /** Called by ForgotPasswordController after loading this FXML. */
    public void init(String email, String name) {
        this.userEmail = email;
        this.userName  = name;
        infoLabel.setText("Enter the code sent to " + email + " and choose a new password.");
    }

    @FXML
    public void initialize() {
        // bind visible <-> password field
        newPasswordVisible.textProperty().bindBidirectional(newPasswordField.textProperty());
        newPasswordVisible.setVisible(false);
        newPasswordVisible.setManaged(false);

        // digits only, max 6 for code field
        codeField.addEventFilter(KeyEvent.KEY_TYPED, e -> {
            if (!e.getCharacter().matches("[0-9]")) { e.consume(); return; }
            if (codeField.getText().length() >= 6)    e.consume();
        });
    }

    @FXML
    private void togglePassword() {
        passVisible = !passVisible;
        newPasswordField.setVisible(!passVisible);
        newPasswordField.setManaged(!passVisible);
        newPasswordVisible.setVisible(passVisible);
        newPasswordVisible.setManaged(passVisible);
        toggleBtn.setText(passVisible ? "🙈" : "👁");
    }

    @FXML
    private void handleReset() {
        String code       = codeField.getText().trim();
        String newPass    = newPasswordField.getText();
        String confirmPass = confirmPasswordField.getText();

        // validate code
        if (code.length() != 6) {
            showMessage("Please enter the 6-digit reset code.", "red");
            return;
        }

        // validate password
        String passError = ValidationUtils.passwordError(newPass);
        if (passError != null) {
            showMessage(passError, "red");
            return;
        }

        // confirm match
        if (!newPass.equals(confirmPass)) {
            showMessage("Passwords do not match.", "red");
            return;
        }

        // verify the code against DB
        if (!authService.verifyAccount(userEmail, code)) {
            showMessage("❌ Invalid or expired code.", "red");
            codeField.clear();
            return;
        }

        // find user and update password
        User user = userDAO.findByEmail(userEmail);
        if (user == null) {
            showMessage("User not found.", "red");
            return;
        }

        // use AuthService to hash + save (bypasses old-password check since we verified via code)
        boolean updated = userDAO.updatePassword(
                user.getId(),
                org.mindrot.jbcrypt.BCrypt.hashpw(newPass, org.mindrot.jbcrypt.BCrypt.gensalt())
        );

        if (updated) {
            showMessage("✅ Password reset successfully! Redirecting to login...", "#16A34A");
            resetBtn.setDisable(true);
            new Thread(() -> {
                try { Thread.sleep(1500); } catch (InterruptedException ignored) {}
                javafx.application.Platform.runLater(this::goToLogin);
            }).start();
        } else {
            showMessage("❌ Something went wrong. Please try again.", "red");
        }
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

    @FXML
    private void handleBackToLogin() {
        goToLogin();
    }

    private void showMessage(String text, String color) {
        messageLabel.setText(text);
        messageLabel.setStyle("-fx-text-fill: " + color + "; -fx-font-weight: bold; -fx-font-size: 13px;");
    }
}