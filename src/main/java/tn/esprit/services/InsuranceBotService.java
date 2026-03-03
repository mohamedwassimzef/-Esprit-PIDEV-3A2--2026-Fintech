package tn.esprit.services;

import tn.esprit.entities.InsurancePackage;
import tn.esprit.entities.InsuredAsset;
import tn.esprit.entities.User;

import java.util.List;

/**
 * ==============================================================================
 *  InsuranceBotService
 *
 *  Wraps AIService with an insurance-specific system prompt.
 *  The prompt is built once in the constructor from:
 *    - Available insurance packages (name, type, price, duration, coverage)
 *    - User information (name, assets they own)
 *    - Asset details (type, declared value, location, manufacture date)
 *
 *  The bot helps the user pick the right package for their asset.
 * ==============================================================================
 */
public class InsuranceBotService {

    private final AIService ai;

    /**
     * @param user      Logged-in user (for name / personalisation)
     * @param assets    User's registered insured assets
     * @param packages  All active insurance packages available
     */
    public InsuranceBotService(User user, List<InsuredAsset> assets, List<InsurancePackage> packages) {
        this.ai = new AIService(buildSystemPrompt(user, assets, packages));
    }

    // ─────────────────────────────────────────────────────────────────────────

    /** Forward a user message and return the bot reply. */
    public String chat(String userMessage) {
        return ai.chat(userMessage);
    }

    /** Reset the conversation history (system prompt is preserved). */
    public void reset() {
        ai.reset();
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  Prompt builder
    // ─────────────────────────────────────────────────────────────────────────

    private String buildSystemPrompt(User user, List<InsuredAsset> assets, List<InsurancePackage> packages) {
        StringBuilder sb = new StringBuilder();

        sb.append("You are FinBot, a friendly and knowledgeable insurance advisor for the FinTech platform.\n");
        sb.append("Your role is to help the user understand and choose the best insurance package for their assets.\n");
        sb.append("Be concise, helpful, and always end by recommending a specific package if you have enough information.\n");
        sb.append("Use the data below — do NOT invent packages or prices.\n\n");

        // ── User info ──────────────────────────────────────────────────────
        sb.append("=== USER INFORMATION ===\n");
        if (user != null) {
            sb.append("Name: ").append(user.getName()).append("\n");
            sb.append("Email: ").append(user.getEmail()).append("\n");
        } else {
            sb.append("Name: Unknown\n");
        }
        sb.append("\n");

        // ── User's assets ──────────────────────────────────────────────────
        sb.append("=== USER'S REGISTERED ASSETS ===\n");
        if (assets == null || assets.isEmpty()) {
            sb.append("The user has no registered assets yet.\n");
        } else {
            for (InsuredAsset a : assets) {
                sb.append("• Reference: ").append(a.getReference())
                  .append(" | Type: ").append(a.getType())
                  .append(" | Declared Value: ").append(
                        a.getDeclaredValue() != null ? a.getDeclaredValue().toPlainString() + " TND" : "N/A")
                  .append(" | Location: ").append(a.getLocation() != null ? a.getLocation() : "N/A")
                  .append(" | Manufacture Date: ").append(
                        a.getManufactureDate() != null ? a.getManufactureDate().toString() : "N/A")
                  .append("\n");
            }
        }
        sb.append("\n");

        // ── Available packages ─────────────────────────────────────────────
        sb.append("=== AVAILABLE INSURANCE PACKAGES ===\n");
        if (packages == null || packages.isEmpty()) {
            sb.append("No packages are currently available.\n");
        } else {
            // Group by asset type for readability
            packages.stream()
                .map(InsurancePackage::getAssetType)
                .distinct()
                .sorted()
                .forEach(type -> {
                    sb.append("-- ").append(type.toUpperCase()).append(" PACKAGES --\n");
                    packages.stream()
                        .filter(p -> type.equals(p.getAssetType()))
                        .forEach(p -> {
                            sb.append("  Package: ").append(p.getName()).append("\n");
                            sb.append("  Base Price: ").append(String.format("%.2f", p.getBasePrice())).append(" TND\n");
                            sb.append("  Risk Multiplier: ").append(p.getRiskMultiplier()).append("\n");
                            sb.append("  Duration: ").append(p.getDurationMonths()).append(" months\n");
                            if (p.getDescription() != null && !p.getDescription().isBlank())
                                sb.append("  Description: ").append(p.getDescription()).append("\n");
                            if (p.getCoverageDetails() != null && !p.getCoverageDetails().isBlank())
                                sb.append("  Coverage: ").append(p.getCoverageDetails()).append("\n");
                            sb.append("\n");
                        });
                });
        }

        // ── Premium formula ────────────────────────────────────────────────
        sb.append("=== PREMIUM CALCULATION FORMULA ===\n");
        sb.append("Premium = basePrice × riskMultiplier × (assetDeclaredValue / 10,000)\n");
        sb.append("Minimum premium is always at least the basePrice.\n\n");

        // ── Bot instructions ───────────────────────────────────────────────
        sb.append("=== YOUR TASK ===\n");
        sb.append("1. Greet the user by name if provided.\n");
        sb.append("2. Ask about the asset they want to insure if not clear.\n");
        sb.append("3. Recommend the most suitable package(s) based on asset type, value, and location.\n");
        sb.append("4. Calculate and show the estimated premium when you recommend a package.\n");
        sb.append("5. Explain briefly why the package is suitable.\n");
        sb.append("6. Answer follow-up questions using only the data provided.\n");
        sb.append("7. If the user asks about a package not in the list, say it is not available.\n");

        return sb.toString();
    }
}


