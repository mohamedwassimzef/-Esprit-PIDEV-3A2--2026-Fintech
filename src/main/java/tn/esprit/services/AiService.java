package tn.esprit.services;

import org.json.JSONArray;
import org.json.JSONObject;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/**
 * AIService -- Chat via GROQ API (llama-3.3-70b-versatile, FREE tier)
 *
 * Usage:
 *   AIService ai = new AIService("You are a helpful assistant...");
 *   String reply = ai.chat("What package suits my car?");
 */
public class AIService {

    private static final String API_KEY = "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";
    private static final String API_URL = "https://openrouter.ai/api/v1/chat/completions";
    private static final String MODEL   = "openai/gpt-4o-mini";

    private static final HttpClient HTTP = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(20))
            .build();

    /** Full conversation history for context-aware multi-turn chat. */
    private final JSONArray history = new JSONArray();

    /** System prompt injected once at construction; never changes per session. */
    private final String systemPrompt;

    /**
     * @param systemPrompt  Instruction context (packages catalogue + user info).
     */
    public AIService(String systemPrompt) {
        this.systemPrompt = systemPrompt;
    }

    // -------------------------------------------------------------------------
    //  PUBLIC API
    // -------------------------------------------------------------------------

    /**
     * Send a user message and return the assistant reply.
     * Conversation history is maintained across calls.
     *
     * @param userMessage  Text typed by the user.
     * @return             Bot reply, or a string starting with "ERROR:" on failure.
     */
    public String chat(String userMessage) {
        history.put(new JSONObject()
                .put("role", "user")
                .put("content", userMessage));

        JSONArray messages = new JSONArray();
        messages.put(new JSONObject()
                .put("role", "system")
                .put("content", systemPrompt));
        for (int i = 0; i < history.length(); i++) {
            messages.put(history.getJSONObject(i));
        }

        String body = new JSONObject()
                .put("model",       MODEL)
                .put("messages",    messages)
                .put("max_tokens",  1024)
                .put("temperature", 0.7)
                .toString();

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(API_URL))
                .header("Authorization", "Bearer " + API_KEY)
                .header("Content-Type",  "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();

        try {
            HttpResponse<String> resp = HTTP.send(request, HttpResponse.BodyHandlers.ofString());

            if (resp.statusCode() != 200) {
                return "ERROR: API status " + resp.statusCode() + " -- " + resp.body();
            }

            String reply = new JSONObject(resp.body())
                    .getJSONArray("choices")
                    .getJSONObject(0)
                    .getJSONObject("message")
                    .getString("content");

            history.put(new JSONObject()
                    .put("role",    "assistant")
                    .put("content", reply));

            return reply;

        } catch (Exception e) {
            return "ERROR: " + e.getMessage();
        }
    }

    /** Clear conversation history (system prompt is preserved). */
    public void reset() {
        history.clear();
    }
}
