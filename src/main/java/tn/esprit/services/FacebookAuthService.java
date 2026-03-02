package tn.esprit.services;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.*;
import javafx.stage.Modality;
import javafx.stage.Stage;
import org.mindrot.jbcrypt.BCrypt;
import tn.esprit.dao.UserDAO;
import tn.esprit.entities.User;

import java.util.concurrent.CompletableFuture;

/**
 * Simulated Facebook authentication dialog.
 *
 * Shows a UI that looks like the Facebook login page.
 * Authenticates against your local database — no real Facebook API call is made.
 * This matches the approach used in the reference project.
 *
 * Behaviour:
 *   - If the email exists in DB → log them in (marks facebookAccount = true)
 *   - If the email does NOT exist → create a new account automatically
 */
public class FacebookAuthService {

    private final UserDAO userDAO = new UserDAO();

    /**
     * Opens the Facebook-styled login dialog.
     * Returns a CompletableFuture<User> completed when the user logs in.
     */
    public CompletableFuture<User> startFacebookAuth() {
        CompletableFuture<User> result = new CompletableFuture<>();

        Platform.runLater(() -> {
            Stage stage = new Stage();
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setTitle("Log in with Facebook");
            stage.setResizable(false);
            stage.setOnCloseRequest(e ->
                    result.completeExceptionally(new RuntimeException("User closed Facebook login")));

            // ── Root layout ──────────────────────────────────────────
            BorderPane root = new BorderPane();

            // ── Header ───────────────────────────────────────────────
            HBox header = new HBox();
            header.setPadding(new Insets(12, 20, 12, 20));
            header.setStyle("-fx-background-color: #1877f2;");
            Text fbLogo = new Text("facebook");
            fbLogo.setFont(Font.font("Arial", FontWeight.BOLD, 28));
            fbLogo.setFill(Color.WHITE);
            header.getChildren().add(fbLogo);
            root.setTop(header);

            // ── Main content ─────────────────────────────────────────
            VBox main = new VBox(24);
            main.setPadding(new Insets(30, 40, 30, 40));
            main.setAlignment(Pos.TOP_CENTER);
            main.setStyle("-fx-background-color: #f0f2f5;");

            // tagline
            Text tagline = new Text("Facebook helps you connect and share with the people in your life.");
            tagline.setWrappingWidth(380);
            tagline.setTextAlignment(TextAlignment.CENTER);
            tagline.setFont(Font.font("Arial", 15));
            tagline.setFill(Color.web("#1c1e21"));

            // ── Login card ───────────────────────────────────────────
            VBox card = new VBox(12);
            card.setPadding(new Insets(18));
            card.setMaxWidth(380);
            card.setStyle("-fx-background-color: white; -fx-background-radius: 8;"
                    + "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.15), 10, 0, 0, 2);");

            TextField emailField = new TextField();
            emailField.setPromptText("Email address or phone number");
            emailField.setPrefHeight(48);
            emailField.setStyle("-fx-background-radius: 6; -fx-border-radius: 6;"
                    + "-fx-border-color: #dddfe2; -fx-font-size: 15px; -fx-padding: 0 10;");

            PasswordField passwordField = new PasswordField();
            passwordField.setPromptText("Password");
            passwordField.setPrefHeight(48);
            passwordField.setStyle("-fx-background-radius: 6; -fx-border-radius: 6;"
                    + "-fx-border-color: #dddfe2; -fx-font-size: 15px; -fx-padding: 0 10;");

            Label errorLabel = new Label();
            errorLabel.setTextFill(Color.web("#e74c3c"));
            errorLabel.setWrapText(true);
            errorLabel.setMaxWidth(360);
            errorLabel.setStyle("-fx-font-size: 13px; -fx-font-weight: bold;");

            Button loginBtn = new Button("Log In");
            loginBtn.setPrefWidth(380);
            loginBtn.setPrefHeight(48);
            loginBtn.setStyle("-fx-background-color: #1877f2; -fx-text-fill: white;"
                    + "-fx-font-weight: bold; -fx-font-size: 17px; -fx-background-radius: 6;"
                    + "-fx-cursor: hand;");
            loginBtn.setOnMouseEntered(e -> loginBtn.setStyle(
                    "-fx-background-color: #166fe5; -fx-text-fill: white;"
                            + "-fx-font-weight: bold; -fx-font-size: 17px; -fx-background-radius: 6; -fx-cursor: hand;"));
            loginBtn.setOnMouseExited(e -> loginBtn.setStyle(
                    "-fx-background-color: #1877f2; -fx-text-fill: white;"
                            + "-fx-font-weight: bold; -fx-font-size: 17px; -fx-background-radius: 6; -fx-cursor: hand;"));

            Separator sep = new Separator();
            sep.setPadding(new Insets(4, 0, 4, 0));

            Button createBtn = new Button("Create new account");
            createBtn.setPrefWidth(200);
            createBtn.setPrefHeight(44);
            createBtn.setStyle("-fx-background-color: #42b72a; -fx-text-fill: white;"
                    + "-fx-font-weight: bold; -fx-font-size: 15px; -fx-background-radius: 6;"
                    + "-fx-cursor: hand;");

            card.getChildren().addAll(emailField, passwordField, errorLabel, loginBtn, sep, createBtn);
            main.getChildren().addAll(tagline, card);
            root.setCenter(main);

            // ── Footer ───────────────────────────────────────────────
            HBox footer = new HBox();
            footer.setPadding(new Insets(10));
            footer.setAlignment(Pos.CENTER);
            footer.setStyle("-fx-background-color: white; -fx-border-color: #dddfe2; -fx-border-width: 1 0 0 0;");
            Text copy = new Text("Meta © 2025");
            copy.setFill(Color.web("#8a8d91"));
            copy.setFont(Font.font("Arial", 12));
            footer.getChildren().add(copy);
            root.setBottom(footer);

            // ── Login action ─────────────────────────────────────────
            loginBtn.setOnAction(e -> {
                String email    = emailField.getText().trim().toLowerCase();
                String password = passwordField.getText();

                if (email.isEmpty() || password.isEmpty()) {
                    errorLabel.setText("Please fill in both fields.");
                    return;
                }
                if (!email.matches("^[\\w.-]+@[\\w.-]+\\.[a-zA-Z]{2,}$")) {
                    errorLabel.setText("Please enter a valid email address.");
                    return;
                }

                loginBtn.setDisable(true);
                errorLabel.setText("");

                new Thread(() -> {
                    User existing = userDAO.findByEmail(email);

                    Platform.runLater(() -> {
                        if (existing != null) {
                            // verify password with BCrypt
                            if (existing.getPasswordHash() != null
                                    && !existing.getPasswordHash().isEmpty()
                                    && BCrypt.checkpw(password, existing.getPasswordHash())) {
                                existing.setFacebookAccount(true);
                                stage.close();
                                result.complete(existing);
                            } else {
                                errorLabel.setText("Incorrect password.");
                                loginBtn.setDisable(false);
                            }
                        } else {
                            // new user — create account from email
                            String userName = email.split("@")[0];
                            if (userName.contains(".")) {
                                String[] parts = userName.split("\\.");
                                userName = capitalize(parts[0]) + (parts.length > 1 ? " " + capitalize(parts[1]) : "");
                            } else {
                                userName = capitalize(userName);
                            }
                            String hashedPw = BCrypt.hashpw(password, BCrypt.gensalt());
                            User newUser = new User(userName, email, hashedPw, 2, "");
                            newUser.setVerified(true);
                            newUser.setFacebookAccount(true);
                            userDAO.create(newUser);
                            User created = userDAO.findByEmail(email);
                            stage.close();
                            result.complete(created);
                        }
                    });
                }).start();
            });

            // pressing Enter in password field triggers login
            passwordField.setOnAction(e -> loginBtn.fire());

            // "Create account" just closes and tells caller nothing happened
            createBtn.setOnAction(e -> {
                stage.close();
                result.completeExceptionally(new RuntimeException("User chose to create a new account instead"));
            });

            stage.setScene(new Scene(root, 460, 560));
            stage.show();
        });

        return result;
    }

    private String capitalize(String s) {
        if (s == null || s.isEmpty()) return s;
        return s.substring(0, 1).toUpperCase() + s.substring(1).toLowerCase();
    }
}