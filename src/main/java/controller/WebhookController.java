package controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tn.esprit.dao.ContractRequestDAO;
import tn.esprit.dao.InsuredContractDAO;
import tn.esprit.entities.ContractRequest;
import tn.esprit.enums.RequestStatus;
import tn.esprit.services.SignedContractProcessor;

/**
 * Spring Boot REST controller that receives BoldSign webhook events.
 *
 * BoldSign webhook configuration:
 *   Public URL : https://iridaceous-misty-vivaciously.ngrok-free.dev/webhook/boldsign
 *   ngrok tunnels → localhost:8081  (Spring Boot port)
 *   Events      : document_completed, document_declined, document_revoked
 *
 * Real BoldSign payload structure:
 * {
 *   "Event": {
 *     "EventType": "document_completed",
 *     "EventTime": "2026-03-02T10:00:00Z"
 *   },
 *   "Data": {
 *     "DocumentId": "xxxx-xxxx-xxxx",
 *     "Title": "Insurance Contract - REF-2026-001",
 *     "Status": "Completed",
 *     ...
 *   }
 * }
 */
@RestController
@RequestMapping("/webhook")
public class WebhookController {

    private final ContractRequestDAO      contractRequestDAO      = new ContractRequestDAO();
    private final InsuredContractDAO      insuredContractDAO      = new InsuredContractDAO();
    private final SignedContractProcessor signedContractProcessor = new SignedContractProcessor();

    /**
     * Main BoldSign webhook receiver.
     * Always returns HTTP 200 so BoldSign does not retry.
     */
    @PostMapping("/boldsign")
    public ResponseEntity<String> handleBoldSignWebhook(
            @RequestBody String payload,
            @RequestHeader(value = "X-BoldSign-Signature", required = false) String signature) {

        System.out.println("[Webhook] ============================================");
        System.out.println("[Webhook] BoldSign event received");
        System.out.println("[Webhook] Raw payload:\n" + payload);

        try {
            // Try PascalCase first (BoldSign standard), then camelCase fallback
            String eventType = firstNonNull(
                extractJsonValue(payload, "EventType"),
                extractJsonValue(payload, "eventType")
            );
            String documentId = firstNonNull(
                extractJsonValue(payload, "DocumentId"),
                extractJsonValue(payload, "documentId")
            );

            System.out.println("[Webhook] EventType  : " + eventType);
            System.out.println("[Webhook] DocumentId : " + documentId);

            if (documentId == null || documentId.isBlank()) {
                System.out.println("[Webhook] No DocumentId found – ignoring.");
                return ResponseEntity.ok("No DocumentId");
            }
            if (eventType == null || eventType.isBlank()) {
                System.out.println("[Webhook] No EventType found – ignoring.");
                return ResponseEntity.ok("No EventType");
            }

            // BoldSign sends eventType as "Completed", "Declined", "Revoked"
            // (NOT "document_completed" — that is only in older docs)
            switch (eventType.toLowerCase()) {
                case "signed",
                     "document_completed",
                     "documentsigned"          -> markAsSigned(documentId);
                case "declined",
                     "document_declined",
                     "documentdeclined"        -> markAsDeclined(documentId);
                case "revoked",
                     "document_revoked",
                     "documentrevoked"         -> markAsDeclined(documentId);
                default -> System.out.println("[Webhook] WARNING - Unhandled event type: '"
                        + eventType + "' — add this to the switch if needed");
            }

        } catch (Exception e) {
            System.out.println("[Webhook] Error: " + e.getMessage());
            e.printStackTrace();
            // Always 200 – never let BoldSign retry on our internal error
        }

        System.out.println("[Webhook] ============================================");
        return ResponseEntity.ok("Received");
    }

    /**
     * Debug endpoint – POST any JSON here and see it printed.
     * Use this to inspect the exact payload BoldSign sends.
     * Example: POST https://.../webhook/debug
     */
    @PostMapping("/debug")
    public ResponseEntity<String> debug(@RequestBody String payload) {
        System.out.println("[Webhook/debug] Raw payload:\n" + payload);
        return ResponseEntity.ok("Debug received:\n" + payload);
    }



    // ── Status updaters ─────────────────────────────────────────────

    private void markAsSigned(String documentId) {
        System.out.println("[Webhook] markAsSigned: " + documentId);

        // 1. Update contract_request.status → SIGNED
        ContractRequest req = contractRequestDAO.findByBoldSignDocumentId(documentId);
        if (req == null) {
            System.out.println("[Webhook] No ContractRequest found for documentId: " + documentId);
        } else {
            System.out.println("[Webhook] Found ContractRequest #" + req.getId()
                    + " (status: " + req.getStatus() + ")");
            boolean ok = contractRequestDAO.updateStatus(req.getId(), RequestStatus.SIGNED);
            System.out.println("[Webhook] ContractRequest #" + req.getId()
                    + (ok ? " -> SIGNED" : " -> update FAILED"));
        }

        // 2. Full post-sign processing:
        //    - resolve asset reference from the ContractRequest
        //    - ensure user folder  contracts/{name}_{id}/  exists  (thread-safe)
        //    - download signed PDF  →  contracts/{name}_{id}/{documentId}.pdf
        //    - create / update insured_contract row with filePath
        signedContractProcessor.process(documentId);
    }

    private void markAsDeclined(String documentId) {
        System.out.println("[Webhook] Looking up request for documentId: " + documentId);

        // 1. Update contract_request status → REJECTED
        ContractRequest req = contractRequestDAO.findByBoldSignDocumentId(documentId);
        if (req == null) {
            System.out.println("[Webhook] No ContractRequest found for documentId: " + documentId);
        } else {
            System.out.println("[Webhook] Found ContractRequest #" + req.getId()
                    + " (current status: " + req.getStatus() + ")");
            boolean ok = contractRequestDAO.updateStatus(req.getId(), RequestStatus.REJECTED);
            System.out.println("[Webhook] ContractRequest #" + req.getId()
                    + (ok ? " -> REJECTED" : " -> DB update FAILED"));
        }

        // 2. Update insured_contract status → REJECTED
        String sql = "UPDATE insured_contract SET status='REJECTED' WHERE boldsign_document_id = ?";
        try (java.sql.Connection c = tn.esprit.utils.MyDB.getInstance().getConx();
             java.sql.PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, documentId);
            int rows = ps.executeUpdate();
            System.out.println("[Webhook] InsuredContract markAsDeclined rows=" + rows);
        } catch (Exception e) {
            System.out.println("[Webhook] InsuredContract decline update error: " + e.getMessage());
        }
    }

    // ── Helpers ──────────────────────────────────────────────────────

    private String firstNonNull(String a, String b) {
        return (a != null && !a.isBlank()) ? a : b;
    }

    /**
     * Extracts the value of the first occurrence of "key" anywhere in the JSON string.
     * Handles both quoted strings and bare values (numbers, booleans).
     * Works regardless of nesting depth.
     */
    private String extractJsonValue(String json, String key) {
        if (json == null || key == null) return null;
        String searchKey = "\"" + key + "\"";
        int idx = json.indexOf(searchKey);
        if (idx < 0) return null;

        int colon = json.indexOf(':', idx + searchKey.length());
        if (colon < 0) return null;

        // Skip whitespace after colon
        int valueStart = colon + 1;
        while (valueStart < json.length()
                && Character.isWhitespace(json.charAt(valueStart))) {
            valueStart++;
        }
        if (valueStart >= json.length()) return null;

        char first = json.charAt(valueStart);

        if (first == '"') {
            // Quoted string – find closing quote, handling escaped quotes
            StringBuilder sb = new StringBuilder();
            int i = valueStart + 1;
            while (i < json.length()) {
                char c = json.charAt(i);
                if (c == '\\' && i + 1 < json.length()) {
                    i += 2; // skip escaped character
                    continue;
                }
                if (c == '"') break;
                sb.append(c);
                i++;
            }
            return sb.length() == 0 ? null : sb.toString();
        } else if (first == '{' || first == '[') {
            // Object or array – not a simple value, skip
            return null;
        } else {
            // Bare value: number, boolean, null
            int end = valueStart;
            while (end < json.length() && ",}\n\r ]".indexOf(json.charAt(end)) < 0) {
                end++;
            }
            String val = json.substring(valueStart, end).trim();
            return val.isEmpty() || val.equals("null") ? null : val;
        }
    }
}









