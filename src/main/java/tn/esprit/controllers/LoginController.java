package tn.esprit.controllers;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.VBox;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.stage.Stage;
import tn.esprit.entities.User;
import tn.esprit.services.AuthService;
import tn.esprit.services.EmailService;
import tn.esprit.services.FacebookAuthService;
import tn.esprit.services.GoogleAuthService;
import tn.esprit.utils.SessionManager;

import java.io.IOException;

public class LoginController {

    // ── Sign In ───────────────────────────────────────────────────────
    @FXML private VBox          signInPane;
    @FXML private TextField     loginEmail;
    @FXML private PasswordField loginPassword;
    @FXML private TextField     loginPasswordVisible;
    @FXML private Button        loginToggleBtn;
    @FXML private Label         loginMessage;
    @FXML private WebView       signInCaptchaView;

    // ── Sign Up ───────────────────────────────────────────────────────
    @FXML private VBox          signUpPane;
    @FXML private TextField     signUpName;
    @FXML private TextField     signUpEmail;
    @FXML private TextField     signUpPhone;
    @FXML private PasswordField signUpPassword;
    @FXML private TextField     signUpPasswordVisible;
    @FXML private Button        signUpToggleBtn;
    @FXML private Label         signUpMessage;
    @FXML private WebView       signUpCaptchaView;

    // ── Tabs ──────────────────────────────────────────────────────────
    @FXML private Button btnSignInTab;
    @FXML private Button btnSignUpTab;

    private boolean loginPassVisible  = false;
    private boolean signUpPassVisible = false;

    // track whether reCAPTCHA was solved
    private boolean signInCaptchaPassed = false;
    private boolean signUpCaptchaPassed = false;

    private final AuthService        authService     = new AuthService();
    private final GoogleAuthService  googleService   = new GoogleAuthService();
    private final FacebookAuthService facebookService = new FacebookAuthService();

    // ── reCAPTCHA site key ────────────────────────────────────────────
    private static final String RECAPTCHA_SITE_KEY = "6LegIngsAAAAADVk_6DaqxpcjYow22cIvuxA0ZRJ";

    private static final String TAB_ACTIVE   =
            "-fx-background-color: #1E3A8A; -fx-text-fill: white; -fx-font-weight: bold;" +
                    "-fx-font-size: 14px; -fx-background-radius: 0; -fx-cursor: hand;";
    private static final String TAB_INACTIVE =
            "-fx-background-color: #E5E7EB; -fx-text-fill: #6B7280; -fx-font-weight: bold;" +
                    "-fx-font-size: 14px; -fx-background-radius: 0; -fx-cursor: hand;";

    // ── init ──────────────────────────────────────────────────────────

    @FXML
    public void initialize() {
        // password toggle bindings
        loginPasswordVisible.textProperty().bindBidirectional(loginPassword.textProperty());
        signUpPasswordVisible.textProperty().bindBidirectional(signUpPassword.textProperty());
        loginPasswordVisible.setVisible(false);  loginPasswordVisible.setManaged(false);
        signUpPasswordVisible.setVisible(false); signUpPasswordVisible.setManaged(false);

        // input filters
        signUpPhone.addEventFilter(KeyEvent.KEY_TYPED, e -> {
            if (!e.getCharacter().matches("[0-9]")) { e.consume(); return; }
            if (signUpPhone.getText().length() >= 8) e.consume();
        });
        signUpName.addEventFilter(KeyEvent.KEY_TYPED, e -> {
            if (!e.getCharacter().matches("[a-zA-Z ]")) e.consume();
        });

        // load reCAPTCHA into both WebViews
        loadCaptcha(signInCaptchaView,  token -> signInCaptchaPassed  = true);
        loadCaptcha(signUpCaptchaView,  token -> signUpCaptchaPassed  = true);
    }

    // ── reCAPTCHA ─────────────────────────────────────────────────────

    /**
     * Loads a reCAPTCHA v2 widget inside a WebView.
     * When the user checks the box, Google JS calls our Java bridge
     * which invokes the onSuccess callback with the token.
     */
    private void loadCaptcha(WebView webView, java.util.function.Consumer<String> onSuccess) {
        WebEngine engine = webView.getEngine();

        // Java bridge object exposed to JavaScript as "javaBridge"
        engine.getLoadWorker().stateProperty().addListener((obs, oldState, newState) -> {
            if (newState == javafx.concurrent.Worker.State.SUCCEEDED) {
                // inject our bridge object so JS can call back
                netscape.javascript.JSObject win = (netscape.javascript.JSObject) engine.executeScript("window");
                win.setMember("javaBridge", new CaptchaBridge(onSuccess));
            }
        });

        String html = "<!DOCTYPE html><html><head>"
                + "<style>body{margin:0;padding:8px;background:transparent;}"
                + ".g-recaptcha{transform:scale(0.88);transform-origin:0 0;}</style>"
                + "<script src='https://www.google.com/recaptcha/api.js' async defer></script>"
                + "</head><body>"
                + "<div class='g-recaptcha'"
                + "     data-sitekey='" + RECAPTCHA_SITE_KEY + "'"
                + "     data-callback='onCaptchaSolved'></div>"
                + "<script>"
                + "function onCaptchaSolved(token) { window.javaBridge.onSolved(token); }"
                + "</script></body></html>";

        engine.loadContent(html);
    }

    /** Bridge class — JavaScript calls onSolved(token) on this object. */
    public static class CaptchaBridge {
        private final java.util.function.Consumer<String> callback;
        CaptchaBridge(java.util.function.Consumer<String> callback) { this.callback = callback; }
        /** Called by JavaScript when reCAPTCHA is solved. */
        public void onSolved(String token) {
            Platform.runLater(() -> callback.accept(token));
        }
    }

    // ── tab switching ─────────────────────────────────────────────────

    @FXML private void showSignIn() {
        signInPane.setVisible(true);  signInPane.setManaged(true);
        signUpPane.setVisible(false); signUpPane.setManaged(false);
        btnSignInTab.setStyle(TAB_ACTIVE);
        btnSignUpTab.setStyle(TAB_INACTIVE);
        loginMessage.setText("");
    }

    @FXML private void showSignUp() {
        signUpPane.setVisible(true);  signUpPane.setManaged(true);
        signInPane.setVisible(false); signInPane.setManaged(false);
        btnSignUpTab.setStyle(TAB_ACTIVE);
        btnSignInTab.setStyle(TAB_INACTIVE);
        signUpMessage.setText("");
    }

    // ── password toggles ──────────────────────────────────────────────

    @FXML private void toggleLoginPassword() {
        loginPassVisible = !loginPassVisible;
        loginPassword.setVisible(!loginPassVisible);       loginPassword.setManaged(!loginPassVisible);
        loginPasswordVisible.setVisible(loginPassVisible); loginPasswordVisible.setManaged(loginPassVisible);
        loginToggleBtn.setText(loginPassVisible ? "🙈" : "👁");
    }

    @FXML private void toggleSignUpPassword() {
        signUpPassVisible = !signUpPassVisible;
        signUpPassword.setVisible(!signUpPassVisible);         signUpPassword.setManaged(!signUpPassVisible);
        signUpPasswordVisible.setVisible(signUpPassVisible);   signUpPasswordVisible.setManaged(signUpPassVisible);
        signUpToggleBtn.setText(signUpPassVisible ? "🙈" : "👁");
    }

    // ── sign in ───────────────────────────────────────────────────────

    @FXML
    private void handleSignIn() {
        if (!signInCaptchaPassed) {
            setMessage(loginMessage, "⚠ Please complete the reCAPTCHA first.", "orange");
            return;
        }

        String email    = loginEmail.getText().trim();
        String password = loginPassword.getText();

        if (email.isEmpty() || password.isEmpty()) {
            setMessage(loginMessage, "⚠ Please fill in all fields.", "red");
            return;
        }

        AuthService.LoginResult result = authService.login(email, password);

        switch (result.status) {
            case SUCCESS -> {
                SessionManager.setCurrentUser(result.user);
                EmailService.sendLoginNotification(result.user.getEmail(), result.user.getName());
                navigateAfterLogin(result.user);
            }
            case NOT_VERIFIED ->
                    setMessage(loginMessage, "⚠ Please verify your email before logging in.", "orange");
            case USER_NOT_FOUND, WRONG_PASSWORD ->
                    setMessage(loginMessage, "❌ Wrong email or password.", "red");
            default ->
                    setMessage(loginMessage, "⚠ Please fill in all fields.", "red");
        }
    }

    // ── sign up ───────────────────────────────────────────────────────

    @FXML
    private void handleSignUp() {
        if (!signUpCaptchaPassed) {
            setMessage(signUpMessage, "⚠ Please complete the reCAPTCHA first.", "orange");
            return;
        }

        String name     = signUpName.getText().trim();
        String email    = signUpEmail.getText().trim();
        String phone    = signUpPhone.getText().trim();
        String password = signUpPassword.getText();

        AuthService.RegisterResult result = authService.register(name, email, password, phone, 2);

        if (!result.success) {
            setMessage(signUpMessage, "⚠ " + result.errorMessage, "red");
            return;
        }

        EmailService.sendVerificationEmail(result.user.getEmail(), result.user.getName(), result.verificationCode);

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/Verification.fxml"));
            Parent root = loader.load();
            VerificationController ctrl = loader.getController();
            ctrl.init(result.user.getEmail(), result.user.getName());

            Stage stage = (Stage) signUpEmail.getScene().getWindow();
            stage.setScene(new Scene(root, 900, 600));
            stage.setTitle("FinTech Portal – Verify your email");
            stage.centerOnScreen();
        } catch (IOException e) {
            e.printStackTrace();
            setMessage(signUpMessage, "❌ Registration failed. Try again.", "red");
        }
    }

    // ── Google auth ───────────────────────────────────────────────────

    @FXML
    private void handleGoogleSignIn() {
        googleService.startGoogleAuth()
                .thenAccept(user -> Platform.runLater(() -> {
                    SessionManager.setCurrentUser(user);
                    EmailService.sendLoginNotification(user.getEmail(), user.getName());
                    navigateAfterLogin(user);
                }))
                .exceptionally(ex -> {
                    Platform.runLater(() ->
                            setMessage(loginMessage, "❌ Google sign-in cancelled or failed.", "red"));
                    return null;
                });
    }

    // ── Facebook auth ─────────────────────────────────────────────────

    @FXML
    private void handleFacebookSignIn() {
        facebookService.startFacebookAuth()
                .thenAccept(user -> Platform.runLater(() -> {
                    SessionManager.setCurrentUser(user);
                    EmailService.sendLoginNotification(user.getEmail(), user.getName());
                    navigateAfterLogin(user);
                }))
                .exceptionally(ex -> {
                    Platform.runLater(() ->
                            setMessage(loginMessage, "❌ Facebook sign-in cancelled.", "red"));
                    return null;
                });
    }

    // ── forgot password ───────────────────────────────────────────────

    @FXML
    private void handleForgotPassword() {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/ForgotPassword.fxml"));
            Stage stage = (Stage) loginEmail.getScene().getWindow();
            stage.setScene(new Scene(root, 900, 600));
            stage.setTitle("FinTech Portal – Forgot Password");
            stage.centerOnScreen();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // ── helpers ───────────────────────────────────────────────────────

    private void navigateAfterLogin(User user) {
        String fxml = SessionManager.isAdmin() ? "UserManagement.fxml" : "UserHome.fxml";
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/" + fxml));
            Parent root = loader.load();
            if (!SessionManager.isAdmin()) {
                UserHomeController ctrl = loader.getController();
                ctrl.setUser(user);
            }
            Stage stage = (Stage) loginEmail.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle("FinTech Portal");
            stage.centerOnScreen();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void setMessage(Label label, String text, String color) {
        label.setText(text);
        label.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: " + color + ";");
    }
}