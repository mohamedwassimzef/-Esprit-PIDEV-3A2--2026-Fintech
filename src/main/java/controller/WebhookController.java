package controller;

import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/webhook")
public class WebhookController {

    @PostMapping("/boldsign")
    public String handleBoldSignWebhook(@RequestBody String payload) {
        // 1. Parse the webhook payload
        // 2. Update your database / notify JavaFX via service
        System.out.println("Webhook received: " + payload);

        // 3. Return 200 OK to BoldSign
        return "Received";
    }
}