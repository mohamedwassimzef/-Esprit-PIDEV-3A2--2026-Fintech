package controller;

import com.sun.net.httpserver.HttpServer;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.VBox;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import tn.esprit.entities.User;
import tn.esprit.services.AuthenticationService;
import tn.esprit.services.SessionManager;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;

/**
 * Controller for Login / Sign-Up screen -- includes reCAPTCHA v2 via
 * a popup window with an embedded localhost HTTP server so Google
 * accepts the origin.
 */
public class LoginController {

    // -- Sign-In fields --
    @FXML private TextField     emailField;
    @FXML private PasswordField passwordField;
    @FXML private TextField     passwordVisible;
    @FXML private Button        togglePasswordBtn;
    @FXML private Label         errorLabel;
    @FXML private Label         titleLabel;
    @FXML private Button        loginButton;
    @FXML private Label         toggleLabel;
    @FXML private Button        toggleButton;

    // -- Sign-Up fields (hidden initially) --
    @FXML private VBox          signupFields;
    @FXML private VBox          confirmPasswordBox;
    @FXML private TextField     nameField;
    @FXML private TextField     phoneField;
    @FXML private PasswordField confirmPasswordField;

    // -- State --
    private boolean isSignUpMode  = false;
    private boolean passwordShown = false;
    private boolean captchaDone   = false;

    private HttpServer captchaServer;
    private int        captchaPort;
    private Stage      captchaStage;

    private static final String RECAPTCHA_SITE_KEY = "6LegIngsAAAAADVk_6DaqxpcjYow22cIvuxA0ZRJ";

    private final AuthenticationService authService = new AuthenticationService();

    // -- Callback to run after captcha is solved --
    private Runnable onCaptchaSolved;

    // -- initialize --

    @FXML
    public void initialize() {
        // Bind visible text field to password field so toggle works
        if (passwordVisible != null) {
            passwordVisible.textProperty().bindBidirectional(passwordField.textProperty());
            passwordVisible.setVisible(false);
            passwordVisible.setManaged(false);
        }

        // Phone: digits only, max 8
        if (phoneField != null) {
            phoneField.addEventFilter(KeyEvent.KEY_TYPED, e -> {
                if (!e.getCharacter().matches("[0-9]")) { e.consume(); return; }
                if (phoneField.getText().length() >= 8) e.consume();
            });
        }

        // Name: letters + space only
        if (nameField != null) {
            nameField.addEventFilter(KeyEvent.KEY_TYPED, e -> {
                if (!e.getCharacter().matches("[a-zA-Z ]")) e.consume();
            });
        }
    }

    // -- Captcha popup window --

    /**
     * Opens a modal popup containing a WebView with the Google reCAPTCHA.
     * Once the user solves it, the popup closes automatically and the
     * provided callback is executed.
     */
    private void openCaptchaWindow(Runnable afterSolved) {
        onCaptchaSolved = afterSolved;
        captchaDone = false;

        try {
            startCaptchaServer();
        } catch (IOException e) {
            System.err.println("[reCAPTCHA] Server failed to start - captcha bypassed: " + e.getMessage());
            captchaDone = true;
            if (afterSolved != null) afterSolved.run();
            return;
        }

        // Build the popup Stage
        captchaStage = new Stage();
        captchaStage.initModality(Modality.APPLICATION_MODAL);
        captchaStage.initStyle(StageStyle.UNDECORATED);
        captchaStage.setTitle("Verify you are human");
        captchaStage.setResizable(false);

        Label header = new Label("Please complete the CAPTCHA");
        header.setStyle("-fx-text-fill:#f5c800;-fx-font-size:14px;-fx-font-weight:700;");

        WebView webView = new WebView();
        webView.setPrefSize(340, 120);
        webView.setMaxSize(340, 120);

        Label hint = new Label("Solve the challenge above to continue");
        hint.setStyle("-fx-text-fill:#888;-fx-font-size:10px;");

        Button cancelBtn = new Button("Cancel");
        cancelBtn.setStyle(
            "-fx-background-color:#222;-fx-text-fill:#aaa;-fx-font-size:11px;" +
            "-fx-padding:6 20;-fx-cursor:hand;-fx-border-color:#333;-fx-border-radius:4;" +
            "-fx-background-radius:4;"
        );
        cancelBtn.setOnAction(e -> {
            stopCaptchaServer();
            captchaStage.close();
        });

        VBox root = new VBox(12, header, webView, hint, cancelBtn);
        root.setAlignment(Pos.CENTER);
        root.setPadding(new Insets(24, 28, 20, 28));
        root.setStyle(
            "-fx-background-color:#0d0d0d;" +
            "-fx-border-color:#f5c800;" +
            "-fx-border-width:2;" +
            "-fx-border-radius:10;" +
            "-fx-background-radius:10;"
        );

        Scene scene = new Scene(root);
        captchaStage.setScene(scene);

        // Load the captcha page
        WebEngine engine = webView.getEngine();
        engine.load("http://localhost:" + captchaPort + "/captcha");

        captchaStage.showAndWait();
    }

    // -- Embedded HTTP server (for captcha only) --

    private void startCaptchaServer() throws IOException {
        // Find a free port
        for (int port = 7500; port < 8000; port++) {
            try {
                captchaServer = HttpServer.create(new InetSocketAddress("localhost", port), 0);
                captchaPort = port;
                break;
            } catch (IOException ignored) {}
        }
        if (captchaServer == null) throw new IOException("No free port found for captcha server");

        // Serve the reCAPTCHA HTML page
        captchaServer.createContext("/", exchange -> {
            String html  = buildCaptchaHtml(captchaPort);
            byte[] bytes = html.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "text/html; charset=utf-8");
            exchange.sendResponseHeaders(200, bytes.length);
            try (OutputStream os = exchange.getResponseBody()) { os.write(bytes); }
        });

        // Receive solved token from JS callback
        captchaServer.createContext("/captcha-solved", exchange -> {
            exchange.sendResponseHeaders(200, 0);
            exchange.getResponseBody().close();

            Platform.runLater(() -> {
                System.out.println("[reCAPTCHA] Captcha solved!");
                captchaDone = true;
                stopCaptchaServer();
                if (captchaStage != null) captchaStage.close();
                if (onCaptchaSolved != null) onCaptchaSolved.run();
            });
        });

        captchaServer.start();
        System.out.println("[reCAPTCHA] Captcha server started on port " + captchaPort);
    }

    private String buildCaptchaHtml(int port) {
        return "<!DOCTYPE html><html><head>"
             + "<meta charset='utf-8'/>"
             + "<style>"
             + "  body { margin:0; padding:12px; background:#0d0d0d; display:flex;"
             + "         justify-content:center; align-items:center; min-height:80px; }"
             + "  .g-recaptcha { transform:scale(0.92); transform-origin:center center; }"
             + "</style>"
             + "<script src='https://www.google.com/recaptcha/api.js' async defer></script>"
             + "</head><body>"
             + "<div class='g-recaptcha'"
             + "     data-sitekey='" + RECAPTCHA_SITE_KEY + "'"
             + "     data-callback='onSolved'"
             + "     data-theme='dark'></div>"
             + "<script>"
             + "function onSolved(token){"
             + "  fetch('http://localhost:" + port + "/captcha-solved?token='+token);"
             + "}"
             + "</script></body></html>";
    }

    // -- Password toggle --

    @FXML
    private void handleTogglePassword() {
        passwordShown = !passwordShown;
        passwordField.setVisible(!passwordShown);
        passwordField.setManaged(!passwordShown);
        if (passwordVisible != null) {
            passwordVisible.setVisible(passwordShown);
            passwordVisible.setManaged(passwordShown);
        }
        if (togglePasswordBtn != null)
            togglePasswordBtn.setText(passwordShown ? "\uD83D\uDE48" : "\uD83D\uDC41");
    }

    // -- Sign-In --

    @FXML
    private void handleLogin() {
        String email    = emailField.getText().trim();
        String password = passwordField.getText();

        if (email.isEmpty() || password.isEmpty()) {
            showError("Please enter both email and password.");
            return;
        }

        // Open captcha popup, then proceed on success
        openCaptchaWindow(() -> doLogin(email, password));
    }

    private void doLogin(String email, String password) {
        if (!captchaDone) {
            showError("Captcha verification failed. Please try again.");
            return;
        }

        User user = authService.authenticate(email, password);
        if (user == null) {
            showError("Invalid email or password.");
            return;
        }

        SessionManager.getInstance().login(user);
        System.out.println("Logged in as: " + user.getName() + " (role: " + user.getRoleId() + ")");

        if (authService.isAdmin(user)) {
            SceneManager.switchScene("/View/AdminDashboard.fxml", "Admin Dashboard");
        } else {
            SceneManager.switchScene("/View/Home.fxml", "FinTech - Home");
        }
    }

    // -- Sign-Up --

    @FXML
    private void handleSignUp() {
        String name            = nameField.getText().trim();
        String email           = emailField.getText().trim();
        String phone           = phoneField.getText().trim();
        String password        = passwordField.getText();
        String confirmPassword = confirmPasswordField.getText();

        if (name.isEmpty() || email.isEmpty() || password.isEmpty()) {
            showError("Name, email and password are required.");
            return;
        }
        if (!password.equals(confirmPassword)) {
            showError("Passwords do not match.");
            return;
        }
        if (password.length() < 6) {
            showError("Password must be at least 6 characters.");
            return;
        }
        if (!email.matches("^[\\w.+\\-]+@[a-zA-Z0-9.\\-]+\\.[a-zA-Z]{2,}$")) {
            showError("Please enter a valid email address.");
            return;
        }

        // Open captcha popup, then proceed on success
        openCaptchaWindow(() -> doSignUp(name, email, phone, password));
    }

    private void doSignUp(String name, String email, String phone, String password) {
        if (!captchaDone) {
            showError("Captcha verification failed. Please try again.");
            return;
        }

        User saved = authService.register(name, email, password, phone);
        if (saved == null) {
            showError("Registration failed. Email may already be in use.");
            return;
        }

        FXMLLoader loader = SceneManager.switchSceneWithLoader(
                "/View/VerifyEmail.fxml", "Verify Your Email");
        if (loader != null) {
            VerifyEmailController ctrl = loader.getController();
            ctrl.init(saved.getId(), saved.getEmail(), saved.getName());
        }
    }

    // -- Toggle login / signup mode --

    @FXML
    private void handleToggle() {
        if (isSignUpMode) switchToLogin();
        else switchToSignUp();
    }

    @FXML
    private void handleForgotPassword() {
        SceneManager.switchScene("/View/ForgotPassword.fxml", "Forgot Password");
    }

    private void switchToSignUp() {
        isSignUpMode = true;
        titleLabel.setText("CREATE ACCOUNT");
        loginButton.setText("SIGN UP");
        loginButton.setOnAction(e -> handleSignUp());
        signupFields.setVisible(true);   signupFields.setManaged(true);
        confirmPasswordBox.setVisible(true); confirmPasswordBox.setManaged(true);
        toggleLabel.setText("Already have an account?");
        toggleButton.setText("Sign In");
        errorLabel.setText("");
    }

    private void switchToLogin() {
        isSignUpMode = false;
        titleLabel.setText("SIGN IN");
        loginButton.setText("SIGN IN");
        loginButton.setOnAction(e -> handleLogin());
        signupFields.setVisible(false);  signupFields.setManaged(false);
        confirmPasswordBox.setVisible(false); confirmPasswordBox.setManaged(false);
        toggleLabel.setText("Don't have an account?");
        toggleButton.setText("Sign Up");
        errorLabel.setText("");
        if (nameField != null)            nameField.clear();
        if (phoneField != null)           phoneField.clear();
        if (confirmPasswordField != null) confirmPasswordField.clear();
    }

    // -- Helpers --

    private void showError(String msg) {
        errorLabel.setStyle("-fx-text-fill:#cc2200;-fx-font-size:11px;");
        errorLabel.setText(msg);
    }

    private void stopCaptchaServer() {
        if (captchaServer != null) {
            captchaServer.stop(0);
            captchaServer = null;
        }
    }
}
