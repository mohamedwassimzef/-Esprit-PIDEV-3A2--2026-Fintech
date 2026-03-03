
package tn.esprit.services;
import org.json.JSONArray;
import org.json.JSONObject;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;

/**
 * ══════════════════════════════════════════════════════════════════════════════
 *  GeminiAIService — Analyse de transactions via GROQ API (100% GRATUIT)
 * ══════════════════════════════════════════════════════════════════════════════
 *
 *  POURQUOI GROQ ?
 *  → Gratuit sans limite journalière stricte
 *  → Ultra rapide (< 2 secondes)
 *  → Modèle : llama-3.3-70b-versatile (très puissant)
 *  → 14 400 requêtes/jour gratuites
 *
 *  OBTENIR LA CLÉ API GROQ (2 minutes) :
 *  ┌─────────────────────────────────────────────────────────────────┐
 *  │  1. Aller sur https://console.groq.com                          │
 *  │  2. Se connecter (Google ou GitHub)                             │
 *  │  3. API Keys → Create API Key                                   │
 *  │  4. Copier la clé (commence par "gsk_...")                      │
 *  │  5. La coller dans API_KEY ci-dessous                           │
 *  └─────────────────────────────────────────────────────────────────┘
 *
 * ══════════════════════════════════════════════════════════════════════════════
 */
class AIService {

    // ── ⚙️  CLÉ API GROQ — https://console.groq.com ──────────────────────────
    private static final String API_KEY = "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";
    // ─────────────────────────────────────────────────────────────────────────

    private static final String API_URL = "https://api.groq.com/openai/v1/chat/completions";
    private static final String MODEL   = "llama-3.3-70b-versatile";

    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(20))
            .build();

    // ══════════════════════════════════════════════════════════════════════════
    //  Classes résultat et données
    // ══════════════════════════════════════════════════════════════════════════




    // ══════════════════════════════════════════════════════════════════════════
    //  Analyser les transactions
    // ══════════════════════════════════════════════════════════════════════════



}
