package tn.esprit.controllers;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.*;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import tn.esprit.dao.UserDAO;
import tn.esprit.entities.Role;
import tn.esprit.entities.User;
import tn.esprit.services.AuthService;
import tn.esprit.services.FaceRecognitionService;
import tn.esprit.services.UserService;
import tn.esprit.utils.SessionManager;

import java.io.IOException;
import java.time.format.DateTimeFormatter;

public class UserHomeController {

    @FXML private Label     welcomeLabel;
    @FXML private Label     profileName;
    @FXML private Label     profileEmail;
    @FXML private Label     profilePhone;
    @FXML private Label     profileRole;
    @FXML private Label     profileVerified;
    @FXML private Label     profileCreatedAt;
    @FXML private Label     faceStatusLabel;
    @FXML private javafx.scene.image.ImageView avatarView;
    @FXML private VBox          changePasswordSection;
    @FXML private PasswordField oldPasswordField;
    @FXML private PasswordField newPasswordField;
    @FXML private PasswordField confirmPasswordField;
    @FXML private Label         pwdMessage;
    @FXML private Button        savePwdBtn;

    private final UserService           userService  = new UserService();
    private final AuthService           authService  = new AuthService();
    private final FaceRecognitionService faceService = new FaceRecognitionService();
    private final UserDAO               userDAO      = new UserDAO();

    public void setUser(User user) {
        SessionManager.setCurrentUser(user);
        populateView(user);
    }

    @FXML
    public void initialize() {
        if (changePasswordSection != null) {
            changePasswordSection.setVisible(false);
            changePasswordSection.setManaged(false);
        }
        if (SessionManager.isLoggedIn()) populateView(SessionManager.getCurrentUser());
    }

    private void populateView(User user) {
        welcomeLabel.setText("Welcome, " + user.getName() + "!");
        profileName.setText(user.getName());
        profileEmail.setText(user.getEmail());
        profilePhone.setText(user.getPhone() != null ? user.getPhone() : "—");

        Role role = userService.getRoleById(user.getRoleId());
        profileRole.setText(role != null ? role.getRoleName() : "Role #" + user.getRoleId());

        if (user.isVerified()) {
            profileVerified.setText("✅ Verified");
            profileVerified.setStyle("-fx-text-fill: #16A34A; -fx-font-size: 14px; -fx-font-weight: bold;");
        } else {
            profileVerified.setText("⏳ Not verified");
            profileVerified.setStyle("-fx-text-fill: #D97706; -fx-font-size: 14px;");
        }

        if (user.getCreatedAt() != null)
            profileCreatedAt.setText(user.getCreatedAt().format(DateTimeFormatter.ofPattern("dd MMM yyyy")));
        else profileCreatedAt.setText("—");

        if (faceStatusLabel != null) {
            if (user.isFaceRegistered()) {
                faceStatusLabel.setText("✅ Enrolled");
                faceStatusLabel.setStyle("-fx-text-fill: #16A34A; -fx-font-weight: bold; -fx-font-size: 14px;");
            } else {
                faceStatusLabel.setText("❌ Not enrolled");
                faceStatusLabel.setStyle("-fx-text-fill: #6B7280; -fx-font-size: 14px;");
            }
        }

        loadAvatar(user.getName());
    }

    /** DiceBear pixel-art avatar - free, no API key, deterministic per name. */
    private void loadAvatar(String name) {
        if (avatarView == null) return;
        new Thread(() -> {
            try {
                String seed = java.net.URLEncoder.encode(name, "UTF-8");
                String url  = "https://api.dicebear.com/9.x/pixel-art/png?seed=" + seed + "&size=96";
                javafx.scene.image.Image img = new javafx.scene.image.Image(url, 96, 96, true, true, true);
                Platform.runLater(() -> avatarView.setImage(img));
            } catch (Exception e) {
                System.err.println("[Avatar] " + e.getMessage());
            }
        }).start();
    }

    @FXML
    private void toggleChangePassword() {
        boolean show = !changePasswordSection.isVisible();
        changePasswordSection.setVisible(show);
        changePasswordSection.setManaged(show);
        if (!show) { oldPasswordField.clear(); newPasswordField.clear(); confirmPasswordField.clear(); pwdMessage.setText(""); }
    }

    @FXML
    private void handleSavePassword() {
        String oldPw  = oldPasswordField.getText();
        String newPw  = newPasswordField.getText();
        String confPw = confirmPasswordField.getText();
        if (oldPw.isEmpty() || newPw.isEmpty() || confPw.isEmpty()) { setPwdMsg("⚠ Fill in all fields.", "red"); return; }
        if (!newPw.equals(confPw)) { setPwdMsg("❌ Passwords do not match.", "red"); return; }
        if (newPw.length() < 6)   { setPwdMsg("❌ At least 6 characters.", "red"); return; }
        savePwdBtn.setDisable(true);
        User user = SessionManager.getCurrentUser();
        new Thread(() -> {
            boolean ok = authService.changePassword(user, oldPw, newPw);
            Platform.runLater(() -> {
                if (ok) {
                    setPwdMsg("✅ Password updated!", "#16A34A");
                    oldPasswordField.clear(); newPasswordField.clear(); confirmPasswordField.clear();
                    new Thread(() -> { try { Thread.sleep(1800); } catch (InterruptedException ig) {} Platform.runLater(this::toggleChangePassword); }).start();
                } else { setPwdMsg("❌ Current password wrong or new password invalid.", "red"); }
                savePwdBtn.setDisable(false);
            });
        }).start();
    }

    @FXML
    private void handleEnrollFace() {
        User user = SessionManager.getCurrentUser();
        if (faceStatusLabel != null) faceStatusLabel.setText("📷 Capturing—look at the camera...");
        new Thread(() -> {
            boolean ok = faceService.enrollFace(user.getId());
            Platform.runLater(() -> {
                if (ok) {
                    userDAO.setFaceRegistered(user.getId(), true);
                    user.setFaceRegistered(true);
                    faceStatusLabel.setText("✅ Face enrolled!");
                    faceStatusLabel.setStyle("-fx-text-fill: #16A34A; -fx-font-weight: bold; -fx-font-size: 14px;");
                } else {
                    faceStatusLabel.setText("❌ Enrolment failed. Check webcam and lighting.");
                    faceStatusLabel.setStyle("-fx-text-fill: red; -fx-font-size: 14px;");
                }
            });
        }).start();
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
        } catch (IOException e) { e.printStackTrace(); }
    }

    private void setPwdMsg(String text, String color) {
        pwdMessage.setText(text);
        pwdMessage.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: " + color + ";");
    }
}
