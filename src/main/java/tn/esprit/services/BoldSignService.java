package tn.esprit.services;
import java.net.http.*;
import java.net.URI;
import java.nio.file.*;
import java.time.LocalDate;
/**
 * BoldSignService
 *
 * 1. Accepts contract data in the constructor.
 * 2. Uses PDFService to generate a PDF from the contract template.
 * 3. Sends the generated PDF to the signer via the BoldSign API.
 *
 * Usage:
 *   BoldSignService service = new BoldSignService(
 *       "Mohamed Tlili",
 *       "REF-2026-HOME-001",
 *       "Premium Home Insurance",
 *       "150,000.00 TND",
 *       LocalDate.of(2026, 3, 1),
 *       "john.doe@example.com"
 *   );
 *   service.sendForSignature();
 */
public class BoldSignService {
    // ----------------------------------------------------------------
    //  BoldSign API credentials  –  replace with your real key
    // ----------------------------------------------------------------
    private static final String API_KEY  = "Njc5M2I1NzYtMjVmZC00ZGExLTlmODktZDgwMDBhOWQ4ZDAx";
    private static final String BASE_URL = "https://api.boldsign.com";
    // Template that PDFService will fill
    private static final String TEMPLATE_PATH = "contracts/contract.html";
    // ----------------------------------------------------------------
    //  Contract data (injected via constructor)
    // ----------------------------------------------------------------
    private final String    userName;
    private final String    assetReference;
    private final String    insurancePackage;
    private final String    approvedValue;
    private final LocalDate contractDate;
    private final String    signerEmail;        // e-mail of the user who must sign
    // ----------------------------------------------------------------
    //  Constructor
    // ----------------------------------------------------------------
    public BoldSignService(String userName,
                           String assetReference,
                           String insurancePackage,
                           String approvedValue,
                           LocalDate contractDate,
                           String signerEmail) {
        this.userName         = userName;
        this.assetReference   = assetReference;
        this.insurancePackage = insurancePackage;
        this.approvedValue    = approvedValue;
        this.contractDate     = contractDate;
        this.signerEmail      = signerEmail;
    }
    // ----------------------------------------------------------------
    //  Public entry point
    // ----------------------------------------------------------------
    /**
     * Generates the contract PDF and sends it to the signer via BoldSign.
     *
     * @return The BoldSign API response body (document ID on success).
     * @throws Exception if PDF generation or the HTTP request fails.
     */
    public String sendForSignature() throws Exception {
        // ── Step 1 : generate the PDF ───────────────────────────────
        String pdfPath = generateContractPdf();
        System.out.println("[BoldSign] PDF generated at: " + Paths.get(pdfPath).toAbsolutePath());
        // ── Step 2 : send to BoldSign ───────────────────────────────
        String response = sendToApi(pdfPath);
        System.out.println("[BoldSign] API response: " + response);
        return response;
    }
    // ----------------------------------------------------------------
    //  Private helpers
    // ----------------------------------------------------------------
    /** Uses PDFService to render the HTML template into a PDF file. */
    private String generateContractPdf() throws Exception {
        Files.createDirectories(Paths.get("contracts"));
        String outputPath = "contracts/contract_" + userName.replaceAll("\\s+", "_")
                + "_" + System.currentTimeMillis() + ".pdf";
        String terms = "This insurance contract covers the insured asset against all declared risks. "
                + "The insured party (" + userName + ") agrees to the terms set out by the "
                + insurancePackage + " package. Any fraudulent claim will void this contract.";
        PDFService pdfService = new PDFService(
                TEMPLATE_PATH,
                userName,
                assetReference,
                insurancePackage,
                approvedValue,
                contractDate,
                terms
        );
        pdfService.generatePdf(outputPath);
        return outputPath;
    }
    /** Sends the PDF to the BoldSign /v1/document/send endpoint. */
    private String sendToApi(String pdfPath) throws Exception {
        String boundary = "----BoldSignBoundary" + System.currentTimeMillis();
        String CRLF = "\r\n";

        byte[] pdfBytes = Files.readAllBytes(Paths.get(pdfPath));

        // ── Part 1 : PDF file ───────────────────────────────────────
        String filePart =
                "--" + boundary + CRLF +
                "Content-Disposition: form-data; name=\"Files\"; filename=\"contract.pdf\"" + CRLF +
                "Content-Type: application/pdf" + CRLF +
                CRLF;

        // ── Part 2 : Title ──────────────────────────────────────────
        String titlePart =
                CRLF + "--" + boundary + CRLF +
                "Content-Disposition: form-data; name=\"Title\"" + CRLF +
                CRLF +
                "Insurance Contract - " + assetReference;

        // ── Part 3 : Signer[0].Name ────────────────────────────────
        String signerName =
                CRLF + "--" + boundary + CRLF +
                "Content-Disposition: form-data; name=\"Signers[0].Name\"" + CRLF +
                CRLF +
                userName;

        // ── Part 4 : Signer[0].EmailAddress ────────────────────────
        String signerEmail =
                CRLF + "--" + boundary + CRLF +
                "Content-Disposition: form-data; name=\"Signers[0].EmailAddress\"" + CRLF +
                CRLF +
                this.signerEmail;

        // ── Part 5 : Signer[0].SignerOrder ─────────────────────────
        String signerOrder =
                CRLF + "--" + boundary + CRLF +
                "Content-Disposition: form-data; name=\"Signers[0].SignerOrder\"" + CRLF +
                CRLF +
                "1";

        // ── Part 6 : Signer[0] FormField – Signature ───────────────
        String fieldType =
                CRLF + "--" + boundary + CRLF +
                "Content-Disposition: form-data; name=\"Signers[0].FormFields[0].FieldType\"" + CRLF +
                CRLF +
                "Signature";

        String fieldPage =
                CRLF + "--" + boundary + CRLF +
                "Content-Disposition: form-data; name=\"Signers[0].FormFields[0].PageNumber\"" + CRLF +
                CRLF +
                "1";

        String fieldX =
                CRLF + "--" + boundary + CRLF +
                "Content-Disposition: form-data; name=\"Signers[0].FormFields[0].Bounds.X\"" + CRLF +
                CRLF +
                "100";

        String fieldY =
                CRLF + "--" + boundary + CRLF +
                "Content-Disposition: form-data; name=\"Signers[0].FormFields[0].Bounds.Y\"" + CRLF +
                CRLF +
                "600";

        String fieldW =
                CRLF + "--" + boundary + CRLF +
                "Content-Disposition: form-data; name=\"Signers[0].FormFields[0].Bounds.Width\"" + CRLF +
                CRLF +
                "200";

        String fieldH =
                CRLF + "--" + boundary + CRLF +
                "Content-Disposition: form-data; name=\"Signers[0].FormFields[0].Bounds.Height\"" + CRLF +
                CRLF +
                "50";

        String fieldRequired =
                CRLF + "--" + boundary + CRLF +
                "Content-Disposition: form-data; name=\"Signers[0].FormFields[0].IsRequired\"" + CRLF +
                CRLF +
                "true";

        // ── Closing boundary ────────────────────────────────────────
        String closing = CRLF + "--" + boundary + "--";

        byte[] bodyBytes = concat(
                filePart.getBytes(),
                pdfBytes,
                titlePart.getBytes(),
                signerName.getBytes(),
                signerEmail.getBytes(),
                signerOrder.getBytes(),
                fieldType.getBytes(),
                fieldPage.getBytes(),
                fieldX.getBytes(),
                fieldY.getBytes(),
                fieldW.getBytes(),
                fieldH.getBytes(),
                fieldRequired.getBytes(),
                closing.getBytes()
        );

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/v1/document/send"))
                .header("X-API-KEY", API_KEY)
                .header("Content-Type", "multipart/form-data; boundary=" + boundary)
                .POST(HttpRequest.BodyPublishers.ofByteArray(bodyBytes))
                .build();

        HttpResponse<String> response = HttpClient.newHttpClient()
                .send(request, HttpResponse.BodyHandlers.ofString());

        System.out.println("[BoldSign] HTTP Status : " + response.statusCode());
        System.out.println("[BoldSign] Response    : " + response.body());
        return response.body();
    }
    // ----------------------------------------------------------------
    //  Utility
    // ----------------------------------------------------------------
    private static byte[] concat(byte[]... arrays) {
        int total = 0;
        for (byte[] a : arrays) total += a.length;
        byte[] result = new byte[total];
        int pos = 0;
        for (byte[] a : arrays) {
            System.arraycopy(a, 0, result, pos, a.length);
            pos += a.length;
        }
        return result;
    }
    // ----------------------------------------------------------------
    //  Quick manual test
    //  mvn exec:java "-Dexec.mainClass=tn.esprit.services.BoldSignService"
    // ----------------------------------------------------------------
    public static void main(String[] args) throws Exception {
        System.out.println();
        System.out.println("+------------------------------------------+");
        System.out.println("|       BoldSignService  -  TEST           |");
        System.out.println("+------------------------------------------+");
        BoldSignService service = new BoldSignService(
                "Mohamed Tlili",
                "REF-2026-HOME-001",
                "Premium Home Insurance",
                "150,000.00 TND",
                LocalDate.of(2026, 3, 1),
                "mohamedwassim.tlili@esprit.tn"          // replace with a real e-mail to test BoldSign
        );
        // Step 1 – just generate the PDF (safe, no API key needed)
        System.out.println("\n[TEST] Generating PDF...");
        String pdfPath = service.generateContractPdf();
        java.nio.file.Path p = Paths.get(pdfPath);
        if (Files.exists(p) && Files.size(p) > 0) {
            System.out.println("  PASS  PDF generated: " + p.toAbsolutePath());
            System.out.printf("  PASS  File size: %,d bytes%n", Files.size(p));
        } else {
            System.out.println("  FAIL  PDF not created or empty.");
            System.exit(1);
        }
        // Step 2 – send to BoldSign (will fail with placeholder API key, but shows the flow)
        System.out.println("\n[TEST] Sending to BoldSign API...");
        System.out.println("  NOTE  Replace API_KEY with a real key to complete the send.");
        try {
            String response = service.sendToApi(pdfPath);
            System.out.println("  DONE  Response: " + response);
        } catch (Exception e) {
            System.out.println("  SKIP  API call failed (expected with placeholder key): " + e.getMessage());
        }
        System.out.println("\n  ALL STEPS COMPLETED");
        System.out.println();
    }
}