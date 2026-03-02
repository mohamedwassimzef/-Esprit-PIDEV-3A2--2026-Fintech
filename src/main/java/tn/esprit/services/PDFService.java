package tn.esprit.services;
import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDate;
public class PDFService {
    private final String templatePath;
    private final String userName;
    private final String assetReference;
    private final String packageName;
    private final String approvedValue;
    private final String contractDate;
    private final String termsParagraph;
    public PDFService(String templatePath, String userName, String assetReference, String packageName,
                      String approvedValue, LocalDate contractDate, String termsParagraph) {
        this.templatePath = templatePath;
        this.userName = userName;
        this.assetReference = assetReference;
        this.packageName = packageName;
        this.approvedValue = approvedValue;
        this.contractDate = contractDate.toString();
        this.termsParagraph = termsParagraph;
    }
    public void generatePdf(String outputPdfPath) throws Exception {
        String html = new String(Files.readAllBytes(Paths.get(templatePath)));
        html = html.replace("{{userName}}", userName)
                .replace("{{assetReference}}", assetReference)
                .replace("{{packageName}}", packageName)
                .replace("{{approvedValue}}", approvedValue)
                .replace("{{contractDate}}", contractDate)
                .replace("{{termsParagraph}}", termsParagraph);
        try (OutputStream os = new FileOutputStream(outputPdfPath)) {
            PdfRendererBuilder builder = new PdfRendererBuilder();
            builder.withHtmlContent(html, null);
            builder.toStream(os);
            builder.run();
        }
    }
    // ----------------------------------------------------------------
    //  TEST  -  run with:
    //  mvn exec:java -Dexec.mainClass="tn.esprit.services.PDFService"
    // ----------------------------------------------------------------
    public static void main(String[] args) {
        System.out.println();
        System.out.println("+------------------------------------------+");
        System.out.println("|       PDFService  -  TEST SUITE          |");
        System.out.println("+------------------------------------------+");
        int passed = 0, failed = 0;
        // ── Test 1 : template file exists ───────────────────────────
        System.out.println("\n[TEST 1] Template file exists");
        java.nio.file.Path templatePath = Paths.get("contracts/contract.html");
        if (Files.exists(templatePath)) {
            System.out.println("  PASS  Template found at: " + templatePath.toAbsolutePath());
            passed++;
        } else {
            System.out.println("  FAIL  Template NOT found at: " + templatePath.toAbsolutePath());
            failed++;
        }
        // ── Test 2 : PDF is generated without exception ──────────────
        System.out.println("\n[TEST 2] PDF generation - no exception thrown");
        String outputPath = "contracts/test_output_" + System.currentTimeMillis() + ".pdf";
        try {
            Files.createDirectories(Paths.get("contracts"));
            PDFService service = new PDFService(
                    templatePath.toString(),
                    "Mohamed Tlili",
                    "REF-2026-HOME-001",
                    "Premium Home Insurance",
                    "150,000.00 TND",
                    LocalDate.of(2026, 3, 1),
                    "This insurance contract covers all risks including fire, theft, flooding, " +
                    "and structural damage. The insured party agrees to pay the agreed premium " +
                    "on a monthly basis. Any fraudulent claim will void this contract immediately."
            );
            service.generatePdf(outputPath);
            System.out.println("  PASS  generatePdf() completed without exception");
            passed++;
        } catch (Exception e) {
            System.out.println("  FAIL  Exception thrown: " + e.getMessage());
            e.printStackTrace();
            failed++;
        }
        // ── Test 3 : output PDF file exists on disk ──────────────────
        System.out.println("\n[TEST 3] Output PDF file exists on disk");
        java.nio.file.Path pdfPath = Paths.get(outputPath);
        if (Files.exists(pdfPath)) {
            System.out.println("  PASS  File found: " + pdfPath.toAbsolutePath());
            passed++;
        } else {
            System.out.println("  FAIL  File NOT found: " + pdfPath.toAbsolutePath());
            failed++;
        }
        // ── Test 4 : output PDF has non-zero size ────────────────────
        System.out.println("\n[TEST 4] Output PDF has non-zero file size");
        try {
            long size = Files.size(pdfPath);
            if (size > 0) {
                System.out.printf("  PASS  File size: %,d bytes%n", size);
                passed++;
            } else {
                System.out.println("  FAIL  File exists but is empty (0 bytes)");
                failed++;
            }
        } catch (Exception e) {
            System.out.println("  FAIL  Could not read file size: " + e.getMessage());
            failed++;
        }
        // ── Test 5 : output starts with %PDF magic bytes ─────────────
        System.out.println("\n[TEST 5] Output file is a valid PDF (starts with %PDF)");
        try {
            byte[] header = new byte[4];
            try (java.io.FileInputStream fis = new java.io.FileInputStream(outputPath)) {
                fis.read(header);
            }
            String magic = new String(header);
            if ("%PDF".equals(magic)) {
                System.out.println("  PASS  Magic bytes confirmed: %PDF");
                passed++;
            } else {
                System.out.println("  FAIL  Unexpected header: " + magic);
                failed++;
            }
        } catch (Exception e) {
            System.out.println("  FAIL  Could not read file header: " + e.getMessage());
            failed++;
        }
        // ── Test 6 : placeholder replacement ─────────────────────────
        System.out.println("\n[TEST 6] All placeholders were replaced (no {{ remaining in HTML)");
        try {
            String html = new String(Files.readAllBytes(templatePath));
            html = html.replace("{{userName}}", "Mohamed Tlili")
                       .replace("{{assetReference}}", "REF-2026-HOME-001")
                       .replace("{{packageName}}", "Premium Home Insurance")
                       .replace("{{approvedValue}}", "150,000.00 TND")
                       .replace("{{contractDate}}", "2026-03-01")
                       .replace("{{termsParagraph}}", "Some terms.");
            if (!html.contains("{{")) {
                System.out.println("  PASS  No unreplaced placeholders found");
                passed++;
            } else {
                System.out.println("  FAIL  Unreplaced {{ }} placeholders still present");
                failed++;
            }
        } catch (Exception e) {
            System.out.println("  FAIL  Could not verify template: " + e.getMessage());
            failed++;
        }
        // ── Summary ──────────────────────────────────────────────────
        System.out.println();
        System.out.println("==========================================");
        System.out.printf( "  Results:  %d passed  /  %d failed%n", passed, failed);
        System.out.println("==========================================");
        if (failed == 0) {
            System.out.println("  ALL TESTS PASSED");
            System.out.println("  PDF saved to: " + Paths.get(outputPath).toAbsolutePath());
        } else {
            System.out.println("  SOME TESTS FAILED");
            System.exit(1);
        }
        System.out.println();
    }
}