package tn.esprit.services;

import tn.esprit.dao.ContractRequestDAO;
import tn.esprit.dao.InsuredAssetDAO;
import tn.esprit.dao.InsuredContractDAO;
import tn.esprit.dao.UserDAO;
import tn.esprit.entities.ContractRequest;
import tn.esprit.entities.InsuredContract;
import tn.esprit.entities.User;
import tn.esprit.enums.ContractStatus;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Handles all post-signature processing when BoldSign notifies us
 * that a document has been signed.
 *
 * Steps performed:
 *  1. Look up the ContractRequest by documentId → get userId + assetId.
 *  2. Resolve the asset reference from insured_asset.reference via assetId.
 *  3. Look up the user name + id for the folder name.
 *  4. Ensure the user's folder  contracts/{user_name}_{user_id}/  exists
 *     (thread-safe: uses a per-folder lock to avoid race conditions).
 *  5. Download the signed PDF via BoldSignDownloader into that folder
 *     as  {document_id}.pdf.
 *  6. Save (or update) the insured_contract record with all data and
 *     the local file path.
 */
public class SignedContractProcessor {

    // ── API key shared with BoldSignService ───────────────────────────────────
    private static final String BOLD_API_KEY =
            "Njc5M2I1NzYtMjVmZC00ZGExLTlmODktZDgwMDBhOWQ4ZDAx";

    // Root folder where all user contract folders live
    private static final String CONTRACTS_ROOT = "contracts";

    // Per-folder locks so two simultaneous webhooks for the same user
    // don't race each other when creating the directory.
    private static final ConcurrentHashMap<String, Object> folderLocks =
            new ConcurrentHashMap<>();

    // ── DAOs (each uses a fresh connection) ───────────────────────────────────
    private final ContractRequestDAO contractRequestDAO = new ContractRequestDAO();
    private final InsuredAssetDAO    assetDAO           = new InsuredAssetDAO();
    private final InsuredContractDAO insuredContractDAO = new InsuredContractDAO();
    private final UserDAO            userDAO            = new UserDAO();

    /**
     * Main entry point — call this from the webhook when a document is signed.
     *
     * @param documentId the BoldSign document ID received in the webhook payload
     */
    public void process(String documentId) {
        System.out.println("[SignedContractProcessor] Processing documentId: " + documentId);

        // ── Step 1: resolve ContractRequest ──────────────────────────────────
        ContractRequest req = contractRequestDAO.findByBoldSignDocumentId(documentId);
        if (req == null) {
            System.out.println("[SignedContractProcessor] No ContractRequest found for documentId: "
                    + documentId + " — cannot save InsuredContract.");
            return;
        }
        int userId  = req.getUserId();
        int assetId = req.getAssetId();
        System.out.println("[SignedContractProcessor] ContractRequest #" + req.getId()
                + "  userId=" + userId + "  assetId=" + assetId);

        // ── Step 2: resolve asset reference ──────────────────────────────────
        String assetRef = assetDAO.findReferenceByAssetId(assetId);
        if (assetRef == null) {
            System.out.println("[SignedContractProcessor] WARNING: assetRef not found for assetId="
                    + assetId + ". Using fallback 'ASSET-" + assetId + "'.");
            assetRef = "ASSET-" + assetId;
        }
        System.out.println("[SignedContractProcessor] assetRef=" + assetRef);

        // ── Step 3: resolve user for folder name ─────────────────────────────
        User user = userDAO.read(userId);
        String folderName;
        if (user != null) {
            // Sanitise name: replace spaces and special chars with underscore
            String safeName = user.getName().replaceAll("[^a-zA-Z0-9]", "_");
            folderName = safeName + "_" + userId;
        } else {
            System.out.println("[SignedContractProcessor] WARNING: user not found for userId="
                    + userId + ". Using fallback folder name.");
            folderName = "user_" + userId;
        }
        System.out.println("[SignedContractProcessor] User folder: " + folderName);

        // ── Step 4: create folder (thread-safe) ──────────────────────────────
        String userFolderPath = CONTRACTS_ROOT + "/" + folderName;
        ensureFolderExists(userFolderPath);

        // ── Step 5: download the signed PDF ──────────────────────────────────
        String filePath = userFolderPath + "/" + documentId + ".pdf";
        boolean downloaded = downloadDocument(documentId, filePath);

        // ── Step 6: save / update InsuredContract record ─────────────────────
        // Check if a record already exists (may have been created when the
        // contract was first sent for signature).
        InsuredContract existing = insuredContractDAO.findByDocumentId(documentId);

        if (existing != null) {
            // Record already exists — update status + file path
            System.out.println("[SignedContractProcessor] Updating existing InsuredContract id="
                    + existing.getId());
            insuredContractDAO.markAsSigned(documentId);
            if (downloaded) {
                insuredContractDAO.updateFilePath(documentId, filePath);
                System.out.println("[SignedContractProcessor] File path saved: " + filePath);
            }
        } else {
            // No record yet — create a new one
            System.out.println("[SignedContractProcessor] Creating new InsuredContract record.");
            InsuredContract contract = new InsuredContract(
                    assetRef,
                    documentId,
                    downloaded ? filePath : null
            );
            contract.setStatus(ContractStatus.SIGNED);
            boolean created = insuredContractDAO.createFresh(contract);
            if (created) {
                // Now set signed_at
                insuredContractDAO.markAsSigned(documentId);
                System.out.println("[SignedContractProcessor] InsuredContract created and marked SIGNED.");
            } else {
                System.out.println("[SignedContractProcessor] Failed to create InsuredContract record.");
            }
        }

        System.out.println("[SignedContractProcessor] Done for documentId: " + documentId);
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    /**
     * Creates the directory at {@code path} if it does not already exist.
     * Uses a per-path lock so concurrent webhook calls for the same user
     * don't race each other.
     */
    private void ensureFolderExists(String path) {
        // getOrDefault creates one lock object per unique folder name
        Object lock = folderLocks.computeIfAbsent(path, k -> new Object());
        synchronized (lock) {
            try {
                Path dir = Paths.get(path);
                if (!Files.exists(dir)) {
                    Files.createDirectories(dir);
                    System.out.println("[SignedContractProcessor] Created folder: "
                            + dir.toAbsolutePath());
                } else {
                    System.out.println("[SignedContractProcessor] Folder already exists: "
                            + dir.toAbsolutePath());
                }
            } catch (IOException e) {
                System.out.println("[SignedContractProcessor] Failed to create folder '"
                        + path + "': " + e.getMessage());
            }
        }
    }

    /**
     * Downloads the signed document from BoldSign into {@code savePath}.
     *
     * @return true if the download succeeded, false otherwise
     */
    private boolean downloadDocument(String documentId, String savePath) {
        try {
            System.out.println("[SignedContractProcessor] Downloading document " + documentId
                    + " → " + savePath);
            BoldSignDownloader.downloadDocument(documentId, savePath, BOLD_API_KEY);
            Path p = Paths.get(savePath);
            boolean exists = Files.exists(p) && Files.size(p) > 0;
            System.out.println("[SignedContractProcessor] Download "
                    + (exists ? "OK (" + Files.size(p) + " bytes)" : "FAILED or empty file"));
            return exists;
        } catch (Exception e) {
            System.out.println("[SignedContractProcessor] Download error: " + e.getMessage());
            return false;
        }
    }
}

