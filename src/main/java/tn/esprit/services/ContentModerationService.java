package tn.esprit.services;


import org.json.JSONArray;
import org.json.JSONObject;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

/**
 * ==============================================================================
 *  Content Moderation Service -- Sightengine Text Moderation API
 * ==============================================================================
 *
 *  API USED  : Sightengine Text Moderation  (https://sightengine.com)
 *  ENDPOINT  : POST https://api.sightengine.com/1.0/text/check.json
 *  COST      : 100% FREE -- 2000 calls/month, NO credit card required
 *
 *  -- MODE RULES vs MODE ML --
 *
 *  mode=ml   -> Deep learning model, understands context.
 *              The word "kill" in "kill it on the dance floor" -> NOT flagged.
 *              Problem: racial slurs used directly can score below 0.7 threshold.
 *
 *  mode=rules -> Dictionary-based pattern matching with millions of variations.
 *              "fuck", "nigga", "n*gga", racial slurs -> ALWAYS flagged.
 *              Returns intensity: "low" / "medium" / "high" instead of score.
 *              We block on ANY match of intensity "medium" or "high".
 *
 *  -- CATEGORIES DETECTED (mode=rules) --
 *  profanity   -> insults, obscenity, vulgar words (fuck, shit, etc.)
 *  extremism   -> white supremacist terms, racial slurs, hate group references
 *  violence    -> threats, calls to harm
 *
 *  -- RESPONSE FORMAT (mode=rules) --
 *  {
 *    "status": "success",
 *    "profanity": {
 *      "matches": [
 *        { "type": "sexual", "intensity": "high", "match": "fuck", "start": 0, "end": 4 }
 *      ]
 *    },
 *    "extremism": {
 *      "matches": [
 *        { "type": "discriminatory", "intensity": "high", "match": "niggas", "start": 18, "end": 24 }
 *      ]
 *    },
 *    "violence": { "matches": [] }
 *  }
 *
 * ==============================================================================
 */
public class ContentModerationService {

    // -- YOUR SIGHTENGINE CREDENTIALS --
    private static final String API_USER   = "53418850";
    private static final String API_SECRET = "7PFYeMEziBmNn8PH2KLQ5JzKepYtARTR";
    // --

    private static final String API_URL = "https://api.sightengine.com/1.0/text/check.json";

    /**
     * Intensity levels returned by rules mode:
     *   "low"    -> mild content (might be acceptable)
     *   "medium" -> clearly inappropriate
     *   "high"   -> very offensive, always block
     *
     * We block on "medium" AND "high" (= skip "low" only).
     */
    private static final String BLOCK_FROM_INTENSITY = "medium"; // "low" = strictest

    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    // --------------------------------------------------------------------------
    //  Result class
    // --------------------------------------------------------------------------

    public static class ModerationResult {
        private final boolean flagged;
        private final String  reason;
        private final String  detectedType;
        private final String  intensity;

        ModerationResult(boolean flagged, String reason, String detectedType, String intensity) {
            this.flagged      = flagged;
            this.reason       = reason;
            this.detectedType = detectedType;
            this.intensity    = intensity;
        }

        public boolean isFlagged()       { return flagged;      }
        public String  getReason()       { return reason;       }
        public String  getDetectedType() { return detectedType; }
        public String  getIntensity()    { return intensity;    }

        @Override
        public String toString() {
            return "ModerationResult{flagged=" + flagged
                    + ", type='" + detectedType + "'"
                    + ", intensity='" + intensity + "'}";
        }
    }

    // --------------------------------------------------------------------------
    //  Main method -- called from AddComplaintController background thread
    // --------------------------------------------------------------------------

    public ModerationResult analyse(String text) {

        if (API_USER.startsWith("PASTE_") || API_USER.isBlank()
                || API_SECRET.startsWith("PASTE_") || API_SECRET.isBlank()) {
            throw new RuntimeException(
                    "Le service de modération n'est pas configuré.\n" +
                            "Veuillez contacter l'administrateur."
            );
        }

        try {
            // -- Build POST form body --
            //
            // mode=rules   -> dictionary-based, reliable slur detection
            // categories   -> what to check (profanity + extremism + violence)
            // lang=en,fr   -> supported languages (no Arabic in rules mode)
            //
            String formBody = "text="       + URLEncoder.encode(text, StandardCharsets.UTF_8)
                    + "&lang="       + "en,fr"
                    + "&mode="       + "rules"
                    + "&categories=" + "profanity,extremism,violence"
                    + "&api_user="   + URLEncoder.encode(API_USER,   StandardCharsets.UTF_8)
                    + "&api_secret=" + URLEncoder.encode(API_SECRET, StandardCharsets.UTF_8);

            System.out.println("[OUT] Sightengine [rules] -> checking: "
                    + text.substring(0, Math.min(text.length(), 80)));

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(API_URL))
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .timeout(Duration.ofSeconds(20))
                    .POST(HttpRequest.BodyPublishers.ofString(formBody))
                    .build();

            HttpResponse<String> response = HTTP_CLIENT.send(
                    request, HttpResponse.BodyHandlers.ofString());

            int status = response.statusCode();
            System.out.println("[IN] Status: " + status);
            System.out.println("[IN] Body:   " + response.body());

            if (status == 400) {
                throw new RuntimeException("Identifiants API invalides (400).\nVérifiez API_USER et API_SECRET.");
            }
            if (status == 401 || status == 403) {
                throw new RuntimeException("Identifiants API Sightengine invalides.");
            }
            if (status == 429) {
                throw new RuntimeException("Quota mensuel atteint (2000 appels/mois gratuits).");
            }
            if (status != 200) {
                throw new RuntimeException("Erreur du service de modération (code " + status + ").");
            }

            return parseResponse(response.body());

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("La vérification a été interrompue.");
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException(
                    "Impossible de joindre le service de modération.\n" +
                            "Vérifiez votre connexion internet.");
        }
    }

    // --------------------------------------------------------------------------
    //  Parse rules-mode response
    // --------------------------------------------------------------------------

    /**
     * In rules mode, each match has:
     *   "type":      "sexual" / "discriminatory" / "insult" / "violence" / etc.
     *   "intensity": "low" / "medium" / "high"
     *   "match":     the exact flagged word/phrase
     *
     * Intensity ranking:  low(1) < medium(2) < high(3)
     * We block if intensity >= BLOCK_FROM_INTENSITY ("medium")
     */
    private ModerationResult parseResponse(String jsonBody) {

        JSONObject root = new JSONObject(jsonBody);

        if (!"success".equals(root.optString("status", ""))) {
            throw new RuntimeException("Réponse inattendue: " + jsonBody);
        }

        // Categories + French error messages
        String[][] categories = {
                {"profanity",  "Votre message contient des grossieretes ou insultes."},
                {"extremism",  "Votre message contient des propos haineux, racistes ou discriminatoires."},
                {"violence",   "Votre message contient des menaces ou propos violents."}
        };

        System.out.println("[MODERATION] Sightengine matches:");

        // Track the worst match found
        int    worstLevel   = 0; // 0=none, 1=low, 2=medium, 3=high
        String worstType    = "none";
        String worstWord    = "";
        String worstMessage = "";

        for (String[] cat : categories) {
            String     catName = cat[0];
            JSONObject catObj  = root.optJSONObject(catName);
            if (catObj == null) continue;

            JSONArray matches = catObj.optJSONArray("matches");
            if (matches == null) continue;

            for (int i = 0; i < matches.length(); i++) {
                JSONObject m         = matches.getJSONObject(i);
                String     intensity = m.optString("intensity", "low");
                String     word      = m.optString("match", "");
                String     type      = m.optString("type", "");
                int        level     = intensityLevel(intensity);

                System.out.printf("   [%-10s][%-15s] %-25s -> %s%n",
                        catName, type, word, intensity.toUpperCase());

                if (level > worstLevel) {
                    worstLevel   = level;
                    worstType    = catName + "/" + type;
                    worstWord    = word;
                    worstMessage = cat[1];
                }
            }
        }

        System.out.printf("[MODERATION] Worst: [%s] '%s' intensity=%s (block from %s)%n",
                worstType, worstWord, levelName(worstLevel), BLOCK_FROM_INTENSITY.toUpperCase());

        // Block if intensity >= our threshold
        if (worstLevel >= intensityLevel(BLOCK_FROM_INTENSITY)) {
            System.out.println("[BLOCKED] " + worstType + " / " + levelName(worstLevel));
            String full = worstMessage + "\n\n"
                    + "Veuillez reformuler votre reclamation de maniere respectueuse.\n"
                    + "Les reclamations inappropriees ne peuvent pas etre soumises.";
            return new ModerationResult(true, full, worstType, levelName(worstLevel));
        }

        System.out.println("[OK] Approved.");
        return new ModerationResult(false, "", "none", "none");
    }

    // -- Helpers --

    private int intensityLevel(String intensity) {
        return switch (intensity.toLowerCase()) {
            case "high"   -> 3;
            case "medium" -> 2;
            case "low"    -> 1;
            default       -> 0;
        };
    }

    private String levelName(int level) {
        return switch (level) {
            case 3  -> "high";
            case 2  -> "medium";
            case 1  -> "low";
            default -> "none";
        };
    }
}
