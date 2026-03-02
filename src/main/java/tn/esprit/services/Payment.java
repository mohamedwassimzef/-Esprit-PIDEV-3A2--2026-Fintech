package tn.esprit.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class Payment {

    private static final String API_KEY = "0d2217842d3c8a1a9639bea1e6e0272f6ec93e4f";
    private static final String CREATE_PAYMENT_URL = "https://sandbox.paymee.tn/api/v2/payments/create";

    // 1. Define a Record for the request body (Clean & Modern JDK 17)
    public record PaymentRequest(
            double amount, String note, String first_name, String last_name,
            String email, String phone, String return_url, String cancel_url,
            String webhook_url, String order_id
    ) {}

    public static void createPayment(double amount, String note, String firstName, String lastName,
                                     String email, String phone, String returnUrl, String cancelUrl,
                                     String webhookUrl, String orderId) throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        HttpClient client = HttpClient.newHttpClient();

        // 2. Create the data object instead of a raw String
        PaymentRequest paymentRequest = new PaymentRequest(
                amount, note, firstName, lastName,
                email, phone, returnUrl,
                cancelUrl, webhookUrl, orderId
        );

        // 3. Serialize Object to JSON String
        String jsonRequest = mapper.writeValueAsString(paymentRequest);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(CREATE_PAYMENT_URL))
                .header("Content-Type", "application/json")
                .header("Authorization", "Token " + API_KEY)
                .POST(HttpRequest.BodyPublishers.ofString(jsonRequest))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        // 4. Improve Output Readability (Pretty Print)
        Object jsonResponse = mapper.readValue(response.body(), Object.class);
        String prettyResponse = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(jsonResponse);

        System.out.println("Status: " + response.statusCode());
        System.out.println("--- Formatted Response ---");
        System.out.println(prettyResponse);
    }
}