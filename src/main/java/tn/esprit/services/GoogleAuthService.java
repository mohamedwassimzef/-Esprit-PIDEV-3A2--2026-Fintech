package tn.esprit.services;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.web.WebView;
import javafx.stage.Modality;
import javafx.stage.Stage;
import org.mindrot.jbcrypt.BCrypt;
import tn.esprit.dao.UserDAO;
import tn.esprit.entities.User;
import tn.esprit.utils.CallbackServer;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;

/**
 * Real Google OAuth2 flow for a Desktop JavaFX app.
 *
 * Flow:
 *   1. Open a WebView showing Google's login page
 *   2. A local CallbackServer on localhost catches the redirect with the auth code
 *   3. Exchange auth code for access token
 *   4. Fetch user info (email, name) from Google
 *   5. If user exists in DB → return them; if not → create account automatically
 */
public class GoogleAuthService {

    private static final String CLIENT_ID     = "enter the client id";
    private static final String CLIENT_SECRET = "enter the client secret";

    private static final String AUTH_URL     = "https://accounts.google.com/o/oauth2/auth";
    private static final String TOKEN_URL    = "https://oauth2.googleapis.com/token";
    private static final String USERINFO_URL = "https://www.googleapis.com/oauth2/v3/userinfo";

    private final UserDAO      userDAO = new UserDAO();
    private final ObjectMapper mapper  = new ObjectMapper();

    /**
     * Starts the Google auth flow.
     * Returns a CompletableFuture<User> completed on the JavaFX thread.
     * The caller should handle exceptions (user closed the window, network error, etc.)
     */
    public CompletableFuture<User> startGoogleAuth() {
        CompletableFuture<User> result = new CompletableFuture<>();

        Platform.runLater(() -> {
            try {
                CallbackServer callbackServer = new CallbackServer();
                callbackServer.start();

                String authUrl = buildAuthUrl(callbackServer.getRedirectUri());

                // Open Google login in a WebView popup
                WebView webView = new WebView();
                Stage authStage = new Stage();
                authStage.initModality(Modality.APPLICATION_MODAL);
                authStage.setTitle("Sign in with Google");
                authStage.setScene(new Scene(webView, 500, 650));
                authStage.setOnCloseRequest(e ->
                        result.completeExceptionally(new RuntimeException("User closed the Google login window")));
                webView.getEngine().load(authUrl);
                authStage.show();

                // When the callback server gets the code, continue on a background thread
                callbackServer.getAuthCodeFuture()
                        .thenApplyAsync(code -> {
                            Platform.runLater(authStage::close);
                            try {
                                String tokenJson    = exchangeCodeForToken(code, callbackServer.getRedirectUri());
                                JsonNode tokenNode  = mapper.readTree(tokenJson);
                                String accessToken  = tokenNode.get("access_token").asText();

                                String userInfoJson = fetchUserInfo(accessToken);
                                JsonNode info       = mapper.readTree(userInfoJson);

                                String email     = info.has("email")       ? info.get("email").asText()       : "";
                                String firstName = info.has("given_name")  ? info.get("given_name").asText()  : "";
                                String lastName  = info.has("family_name") ? info.get("family_name").asText() : "";
                                String fullName  = (firstName + " " + lastName).trim();

                                // Check if user already exists
                                User existing = userDAO.findByEmail(email.toLowerCase());
                                if (existing != null) return existing;

                                // Create new account — verified by Google, no local password
                                User newUser = new User(
                                        fullName.isEmpty() ? email.split("@")[0] : fullName,
                                        email.toLowerCase(),
                                        BCrypt.hashpw(java.util.UUID.randomUUID().toString(), BCrypt.gensalt()),
                                        2,   // role_id = 2 (regular user)
                                        ""   // no phone
                                );
                                newUser.setVerified(true);
                                newUser.setGoogleAccount(true);
                                userDAO.create(newUser);
                                return userDAO.findByEmail(email.toLowerCase());

                            } catch (Exception e) {
                                throw new RuntimeException("Google auth error: " + e.getMessage(), e);
                            }
                        })
                        .thenAccept(user -> Platform.runLater(() -> result.complete(user)))
                        .exceptionally(ex -> {
                            Platform.runLater(() -> result.completeExceptionally(ex));
                            return null;
                        });

            } catch (Exception e) {
                result.completeExceptionally(e);
            }
        });

        return result;
    }

    // ── private helpers ───────────────────────────────────────────────

    private String buildAuthUrl(String redirectUri) throws UnsupportedEncodingException {
        return AUTH_URL
                + "?client_id="     + URLEncoder.encode(CLIENT_ID,              "UTF-8")
                + "&redirect_uri="  + URLEncoder.encode(redirectUri,            "UTF-8")
                + "&response_type=code"
                + "&scope="         + URLEncoder.encode("email profile",        "UTF-8")
                + "&access_type=offline"
                + "&prompt=select_account";
    }

    private String exchangeCodeForToken(String code, String redirectUri) throws IOException {
        String body = "code="          + URLEncoder.encode(code,         "UTF-8")
                + "&client_id="    + URLEncoder.encode(CLIENT_ID,    "UTF-8")
                + "&client_secret="+ URLEncoder.encode(CLIENT_SECRET,"UTF-8")
                + "&redirect_uri=" + URLEncoder.encode(redirectUri,  "UTF-8")
                + "&grant_type=authorization_code";

        HttpURLConnection conn = (HttpURLConnection) new URL(TOKEN_URL).openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
        conn.setDoOutput(true);
        conn.getOutputStream().write(body.getBytes(StandardCharsets.UTF_8));

        int status = conn.getResponseCode();
        InputStream is = (status == 200) ? conn.getInputStream() : conn.getErrorStream();
        String response = new String(is.readAllBytes(), StandardCharsets.UTF_8);
        if (status != 200) throw new IOException("Token exchange failed (" + status + "): " + response);
        return response;
    }

    private String fetchUserInfo(String accessToken) throws IOException {
        HttpURLConnection conn = (HttpURLConnection) new URL(USERINFO_URL).openConnection();
        conn.setRequestProperty("Authorization", "Bearer " + accessToken);
        int status = conn.getResponseCode();
        String response = new String(conn.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        if (status != 200) throw new IOException("User info fetch failed: " + status);
        return response;
    }
}