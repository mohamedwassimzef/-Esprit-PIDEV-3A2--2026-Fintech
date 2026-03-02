package tn.esprit.utils;

import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.util.concurrent.CompletableFuture;

/**
 * Tiny embedded HTTP server that listens on localhost for the
 * Google OAuth redirect and extracts the authorization code.
 *
 * Lifecycle: start() → wait for getAuthCodeFuture() → server shuts itself down.
 */
public class CallbackServer {

    private HttpServer server;
    private final CompletableFuture<String> authCodeFuture = new CompletableFuture<>();
    private int port;

    public CallbackServer() throws IOException {
        // try ports 8000-8999 until one is free
        for (int tryPort = 8000; tryPort < 9000; tryPort++) {
            try {
                server = HttpServer.create(new InetSocketAddress(tryPort), 0);
                this.port = tryPort;
                break;
            } catch (IOException ignored) {}
        }
        if (server == null) throw new IOException("No free port found in range 8000-8999");

        server.createContext("/callback", exchange -> {
            String query = exchange.getRequestURI().getQuery();
            String code  = null;

            if (query != null && query.contains("code=")) {
                code = query.substring(query.indexOf("code=") + 5);
                if (code.contains("&")) code = code.substring(0, code.indexOf("&"));
            }

            String html = "<html><body style='font-family:Arial,sans-serif;text-align:center;margin-top:60px;'>"
                    + "<h2 style='color:#1E3A8A;'>✅ Authentication successful!</h2>"
                    + "<p>You can close this window and return to the app.</p>"
                    + "</body></html>";

            exchange.sendResponseHeaders(200, html.getBytes().length);
            try (OutputStream os = exchange.getResponseBody()) { os.write(html.getBytes()); }

            if (code != null) authCodeFuture.complete(code);
            else authCodeFuture.completeExceptionally(new RuntimeException("No auth code in callback"));

            server.stop(1);
        });
    }

    public void start()                              { server.start(); }
    public CompletableFuture<String> getAuthCodeFuture() { return authCodeFuture; }
    public int getPort()                             { return port; }
    public String getRedirectUri()                   { return "http://localhost:" + port + "/callback"; }
}