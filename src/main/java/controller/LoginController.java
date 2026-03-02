package controller;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import tn.esprit.entities.User;
import tn.esprit.services.AuthenticationService;
import tn.esprit.services.SessionManager;

/**
 * Controller for the Login / Sign-Up screen.
 */
public class LoginController {

    // -- shared fields --
    @FXML private TextField      emailField;
    @FXML private PasswordField  passwordField;
    @FXML private Label          errorLabel;
    @FXML private Label          titleLabel;
    @FXML private Button         loginButton;
    @FXML private Label          toggleLabel;
    @FXML private Button         toggleButton;

    // -- sign-up only fields (hidden during login) --
    @FXML private VBox           signupFields;
    @FXML private VBox           confirmPasswordBox;
    @FXML private TextField      nameField;
    @FXML private TextField      phoneField;
    @FXML private PasswordField  confirmPasswordField;

    private final AuthenticationService authService = new AuthenticationService();
    private boolean isSignUpMode = false;

    @FXML
    private void handleLogin() {
        String email    = emailField.getText().trim();
        String password = passwordField.getText();

        if (email.isEmpty() || password.isEmpty()) {
            errorLabel.setText("Please enter both email and password.");
            return;
        }

        User user = authService.authenticate(email, password);
        if (user == null) {
            errorLabel.setText("Invalid email or password.");
            return;
        }

        SessionManager.getInstance().login(user);
        System.out.println("Logged in as: " + user.getName() + " (role: " + user.getRoleId() + ")");

        if (authService.isAdmin(user)) {
            SceneManager.switchScene("/View/AdminDashboard.fxml", "Admin Dashboard");
        } else {
            SceneManager.switchScene("/View/Home.fxml", "FinTech – Home");
        }
    }

    @FXML
    private void handleSignUp() {
        String name            = nameField.getText().trim();
        String email           = emailField.getText().trim();
        String phone           = phoneField.getText().trim();
        String password        = passwordField.getText();
        String confirmPassword = confirmPasswordField.getText();

        // Validation
        if (name.isEmpty() || email.isEmpty() || password.isEmpty()) {
            errorLabel.setText("Name, email and password are required.");
            return;
        }
        if (!password.equals(confirmPassword)) {
            errorLabel.setText("Passwords do not match.");
            return;
        }
        if (password.length() < 6) {
            errorLabel.setText("Password must be at least 6 characters.");
            return;
        }
        if (!email.matches("^[\\w.+\\-]+@[a-zA-Z0-9.\\-]+\\.[a-zA-Z]{2,}$")) {
            errorLabel.setText("Please enter a valid email address.");
            return;
        }

        boolean success = authService.register(name, email, password, phone);
        if (!success) {
            errorLabel.setText("Registration failed. Email may already be in use.");
            return;
        }

        // Auto-login after successful registration
        User user = authService.authenticate(email, password);
        if (user != null) {
            SessionManager.getInstance().login(user);
            SceneManager.switchScene("/View/UserDashboard.fxml", "User Dashboard");
        } else {
            // Fallback: switch to login mode with success message
            switchToLogin();
            errorLabel.setStyle("-fx-text-fill:#22cc66;-fx-font-size:11px;");
            errorLabel.setText("Account created! Please sign in.");
        }
    }

    @FXML
    private void handleToggle() {
        if (isSignUpMode) {
            switchToLogin();
        } else {
            switchToSignUp();
        }
    }

    private void switchToSignUp() {
        isSignUpMode = true;
        titleLabel.setText("CREATE ACCOUNT");
        loginButton.setText("SIGN UP");
        loginButton.setOnAction(e -> handleSignUp());
        signupFields.setVisible(true);
        signupFields.setManaged(true);
        confirmPasswordBox.setVisible(true);
        confirmPasswordBox.setManaged(true);
        toggleLabel.setText("Already have an account?");
        toggleButton.setText("Sign In");
        errorLabel.setStyle("-fx-text-fill:#cc2200;-fx-font-size:11px;");
        errorLabel.setText("");
    }

    private void switchToLogin() {
        isSignUpMode = false;
        titleLabel.setText("SIGN IN");
        loginButton.setText("SIGN IN");
        loginButton.setOnAction(e -> handleLogin());
        signupFields.setVisible(false);
        signupFields.setManaged(false);
        confirmPasswordBox.setVisible(false);
        confirmPasswordBox.setManaged(false);
        toggleLabel.setText("Don't have an account?");
        toggleButton.setText("Sign Up");
        errorLabel.setStyle("-fx-text-fill:#cc2200;-fx-font-size:11px;");
        errorLabel.setText("");
        // Clear sign-up fields
        nameField.clear();
        phoneField.clear();
        confirmPasswordField.clear();
    }
}
