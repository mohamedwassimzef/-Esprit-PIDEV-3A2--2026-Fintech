package tn.esprit.entities;

import tn.esprit.enums.ContractStatus;
import java.time.LocalDateTime;

/**
 * InsuredContract entity — matches the insured_contract table schema.
 *
 * CREATE TABLE insured_contract (
 *   id                   INT AUTO_INCREMENT PRIMARY KEY,
 *   asset_ref            VARCHAR(150) NOT NULL,
 *   boldsign_document_id VARCHAR(255) NOT NULL,
 *   status               ENUM('NOT_SIGNED','SIGNED','REJECTED') NOT NULL DEFAULT 'NOT_SIGNED',
 *   created_at           TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
 *   signed_at            TIMESTAMP NULL DEFAULT NULL,
 *   local_file_path      VARCHAR(500) DEFAULT NULL,
 *   CONSTRAINT fk_asset FOREIGN KEY (asset_ref) REFERENCES insured_asset(reference) ON DELETE CASCADE,
 *   UNIQUE KEY uk_document_id (boldsign_document_id),
 *   INDEX idx_asset_ref (asset_ref)
 * );
 */
public class InsuredContract {

    private int            id;
    private String         assetRef;            // FK → insured_asset.reference
    private String         boldSignDocumentId;
    private ContractStatus status;
    private LocalDateTime  createdAt;
    private LocalDateTime  signedAt;
    private String         localFilePath;

    // ── Constructors ──────────────────────────────────────────────────────

    public InsuredContract() {}

    /** Full constructor — used when reading from DB. */
    public InsuredContract(int id, String assetRef, String boldSignDocumentId,
                           ContractStatus status, LocalDateTime createdAt,
                           LocalDateTime signedAt, String localFilePath) {
        this.id                 = id;
        this.assetRef           = assetRef;
        this.boldSignDocumentId = boldSignDocumentId;
        this.status             = status;
        this.createdAt          = createdAt;
        this.signedAt           = signedAt;
        this.localFilePath      = localFilePath;
    }

    /** Create constructor — id and createdAt are set by the DB. */
    public InsuredContract(String assetRef, String boldSignDocumentId, String localFilePath) {
        this.assetRef           = assetRef;
        this.boldSignDocumentId = boldSignDocumentId;
        this.status             = ContractStatus.NOT_SIGNED;
        this.localFilePath      = localFilePath;
    }

    // ── Getters & Setters ─────────────────────────────────────────────────

    public int getId()                        { return id; }
    public void setId(int id)                 { this.id = id; }

    public String getAssetRef()               { return assetRef; }
    public void setAssetRef(String assetRef)  { this.assetRef = assetRef; }

    public String getBoldSignDocumentId()     { return boldSignDocumentId; }
    public void setBoldSignDocumentId(String v) { this.boldSignDocumentId = v; }

    public ContractStatus getStatus()         { return status; }
    public void setStatus(ContractStatus s)   { this.status = s; }

    public LocalDateTime getCreatedAt()       { return createdAt; }
    public void setCreatedAt(LocalDateTime v) { this.createdAt = v; }

    public LocalDateTime getSignedAt()        { return signedAt; }
    public void setSignedAt(LocalDateTime v)  { this.signedAt = v; }

    public String getLocalFilePath()          { return localFilePath; }
    public void setLocalFilePath(String v)    { this.localFilePath = v; }

    // ── toString ──────────────────────────────────────────────────────────

    @Override
    public String toString() {
        return "InsuredContract{" +
                "id=" + id +
                ", assetRef='" + assetRef + '\'' +
                ", boldSignDocumentId='" + boldSignDocumentId + '\'' +
                ", status=" + status +
                ", createdAt=" + createdAt +
                ", signedAt=" + signedAt +
                ", localFilePath='" + localFilePath + '\'' +
                '}';
    }
}
