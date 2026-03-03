package tn.esprit.controllers;

import com.sun.net.httpserver.HttpServer;
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
import tn.esprit.services.GoogleAuthService;
import tn.esprit.utils.SessionManager;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;

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

    private boolean signInCaptchaPassed  = false;
    private boolean signUpCaptchaPassed  = false;

    private final AuthService       authService   = new AuthService();
    private final GoogleAuthService googleService = new GoogleAuthService();

    private static final String RECAPTCHA_SITE_KEY = "6LegIngsAAAAADVk_6DaqxpcjYow22cIvuxA0ZRJ";

    // Embedded HTTP server that serves the reCAPTCHA page over localhost
    private HttpServer captchaServer;
    private int        captchaPort;

    private static final String TAB_ACTIVE =
            "-fx-background-color: #1E3A8A; -fx-text-fill: white; -fx-font-weight: bold;" +
                    "-fx-font-size: 14px; -fx-background-radius: 0; -fx-cursor: hand;";
    private static final String TAB_INACTIVE =
            "-fx-background-color: #E5E7EB; -fx-text-fill: #6B7280; -fx-font-weight: bold;" +
                    "-fx-font-size: 14px; -fx-background-radius: 0; -fx-cursor: hand;";

    // ── init ──────────────────────────────────────────────────────────

    @FXML
    public void initialize() {
        // Password toggle bindings
        loginPasswordVisible.textProperty().bindBidirectional(loginPassword.textProperty());
        signUpPasswordVisible.textProperty().bindBidirectional(signUpPassword.textProperty());
        loginPasswordVisible.setVisible(false);  loginPasswordVisible.setManaged(false);
        signUpPasswordVisible.setVisible(false); signUpPasswordVisible.setManaged(false);

        // Input filters
        signUpPhone.addEventFilter(KeyEvent.KEY_TYPED, e -> {
            if (!e.getCharacter().matches("[0-9]")) { e.consume(); return; }
            if (signUpPhone.getText().length() >= 8) e.consume();
        });
        signUpName.addEventFilter(KeyEvent.KEY_TYPED, e -> {
            if (!e.getCharacter().matches("[a-zA-Z ]")) e.consume();
        });

        // Start the embedded HTTP server, then load reCAPTCHA via real localhost URL
        try {
            startCaptchaServer();
            loadCaptcha(signInCaptchaView, "signin",  token -> signInCaptchaPassed = true);
            loadCaptcha(signUpCaptchaView, "signup",  token -> signUpCaptchaPassed = true);
        } catch (IOException e) {
            e.printStackTrace();
            // Fallback: allow login without captcha if server fails to start
            signInCaptchaPassed = true;
            signUpCaptchaPassed = true;
        }
    }

    // ── Embedded HTTP server ──────────────────────────────────────────

    /**
     * Starts a tiny HTTP server on a free port.
     * It serves the reCAPTCHA HTML page at /signin and /signup,
     * and receives the solved token at /captcha-solved?type=signin|signup
     */
    private void startCaptchaServer() throws IOException {
        // Find a free port
        for (int port = 7500; port < 8000; port++) {
            try {
                captchaServer = HttpServer.create(new InetSocketAddress("localhost", port), 0);
                captchaPort = port;
                break;
            } catch (IOException ignored) {}
        }
        if (captchaServer == null) throw new IOException("No free port found");

        // Serve the reCAPTCHA HTML page
        captchaServer.createContext("/", exchange -> {
            String path = exchange.getRequestURI().getPath(); // /signin or /signup
            String html = buildCaptchaHtml(captchaPort);
            byte[] bytes = html.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "text/html; charset=utf-8");
            exchange.sendResponseHeaders(200, bytes.length);
            try (OutputStream os = exchange.getResponseBody()) { os.write(bytes); }
        });

        // Receive the solved token from the JS callback
        captchaServer.createContext("/captcha-solved", exchange -> {
            String query = exchange.getRequestURI().getQuery(); // type=signin&token=XXX
            String type  = null;
            String token = null;
            if (query != null) {
                for (String part : query.split("&")) {
                    if (part.startsWith("type="))  type  = part.substring(5);
                    if (part.startsWith("token=")) token = part.substring(6);
                }
            }
            // Send empty 200 OK back to the JS fetch call
            exchange.sendResponseHeaders(200, 0);
            exchange.getResponseBody().close();

            // Notify JavaFX thread
            final String resolvedType  = type;
            final String resolvedToken = token;
            Platform.runLater(() -> {
                if ("signin".equals(resolvedType))  signInCaptchaPassed  = true;
                if ("signup".equals(resolvedType))  signUpCaptchaPassed  = true;
            });
        });

        captchaServer.start();
    }

    /**
     * Builds the HTML served at http://localhost:{port}/{type}.
     * When reCAPTCHA is solved it POSTs back to /captcha-solved.
     */
    private String buildCaptchaHtml(int port) {
        return "<!DOCTYPE html><html><head>"
                + "<style>body{margin:0;padding:8px;background:transparent;}"
                + ".g-recaptcha{transform:scale(0.88);transform-origin:0 0;}</style>"
                + "<script src='https://www.google.com/recaptcha/api.js' async defer></script>"
                + "</head><body>"
                + "<div class='g-recaptcha'"
                + "     data-sitekey='" + RECAPTCHA_SITE_KEY + "'"
                + "     data-callback='onCaptchaSolved'></div>"
                + "<script>"
                + "function onCaptchaSolved(token) {"
                + "  var type = window.location.pathname.replace('/','');" // signin or signup
                + "  fetch('http://localhost:" + port + "/captcha-solved?type='+type+'&token='+token);"
                + "}"
                + "</script></body></html>";
    }

    // ── Load reCAPTCHA WebView ────────────────────────────────────────

    /**
     * Loads the reCAPTCHA page via a real http://localhost URL so Google
     * accepts the origin. The 'type' path segment (signin/signup) tells
     * the server which flag to set when the token is received.
     */
    private void loadCaptcha(WebView webView, String type,
                             java.util.function.Consumer<String> onSuccess) {
        WebEngine engine = webView.getEngine();
        // Load over real HTTP — Google will see origin = http://localhost
        engine.load("http://localhost:" + captchaPort + "/" + type);
    }

    // ── Tab switching ─────────────────────────────────────────────────

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

    // ── Password toggles ──────────────────────────────────────────────

    @FXML private void toggleLoginPassword() {
        loginPassVisible = !loginPassVisible;
        loginPassword.setVisible(!loginPassVisible);       loginPassword.setManaged(!loginPassVisible);
        loginPasswordVisible.setVisible(loginPassVisible); loginPasswordVisible.setManaged(loginPassVisible);
        loginToggleBtn.setText(loginPassVisible ? "\uD83D\uDE48" : "\uD83D\uDC41");
    }

    @FXML private void toggleSignUpPassword() {
        signUpPassVisible = !signUpPassVisible;
        signUpPassword.setVisible(!signUpPassVisible);        signUpPassword.setManaged(!signUpPassVisible);
        signUpPasswordVisible.setVisible(signUpPassVisible);  signUpPasswordVisible.setManaged(signUpPassVisible);
        signUpToggleBtn.setText(signUpPassVisible ? "\uD83D\uDE48" : "\uD83D\uDC41");
    }

    // ── Sign In ───────────────────────────────────────────────────────

    @FXML
    private void handleSignIn() {
        if (!signInCaptchaPassed) {
            setMessage(loginMessage, "\u26a0 Please complete the reCAPTCHA first.", "orange");
            return;
        }
        String email    = loginEmail.getText().trim();
        String password = loginPassword.getText();
        if (email.isEmpty() || password.isEmpty()) {
            setMessage(loginMessage, "\u26a0 Please fill in all fields.", "red");
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
                    setMessage(loginMessage, "\u26a0 Please verify your email before logging in.", "orange");
            case USER_NOT_FOUND, WRONG_PASSWORD ->
                    setMessage(loginMessage, "\u274c Wrong email or password.", "red");
            default ->
                    setMessage(loginMessage, "\u26a0 Please fill in all fields.", "red");
        }
    }

    // ── Sign Up ───────────────────────────────────────────────────────

    @FXML
    private void handleSignUp() {
        if (!signUpCaptchaPassed) {
            setMessage(signUpMessage, "\u26a0 Please complete the reCAPTCHA first.", "orange");
            return;
        }
        String name     = signUpName.getText().trim();
        String email    = signUpEmail.getText().trim();
        String phone    = signUpPhone.getText().trim();
        String password = signUpPassword.getText();

        AuthService.RegisterResult result = authService.register(name, email, password, phone, 2);
        if (!result.success) {
            setMessage(signUpMessage, "\u26a0 " + result.errorMessage, "red");
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
            stage.setTitle("FinTech Portal \u2013 Verify your email");
            stage.centerOnScreen();
        } catch (IOException e) {
            e.printStackTrace();
            setMessage(signUpMessage, "\u274c Registration failed. Try again.", "red");
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
                            setMessage(loginMessage, "\u274c Google sign-in cancelled or failed.", "red"));
                    return null;
                });
    }

    // ── Face login ────────────────────────────────────────────────────

    @FXML
    private void handleFaceLogin() {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/FaceLogin.fxml"));
            Stage stage = (Stage) loginEmail.getScene().getWindow();
            stage.setScene(new Scene(root, 900, 600));
            stage.setTitle("FinTech Portal \u2013 Face Login");
            stage.centerOnScreen();
        } catch (IOException e) {
            e.printStackTrace();
            setMessage(loginMessage, "\u274c Could not open Face Login.", "red");
        }
    }

    // ── Forgot password ───────────────────────────────────────────────

    @FXML
    private void handleForgotPassword() {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/ForgotPassword.fxml"));
            Stage stage = (Stage) loginEmail.getScene().getWindow();
            stage.setScene(new Scene(root, 900, 600));
            stage.setTitle("FinTech Portal \u2013 Forgot Password");
            stage.centerOnScreen();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────

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