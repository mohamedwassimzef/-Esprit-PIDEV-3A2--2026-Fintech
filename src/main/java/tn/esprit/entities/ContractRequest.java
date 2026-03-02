package tn.esprit.entities;
import tn.esprit.enums.RequestStatus;
import java.time.LocalDateTime;
/**
 * ContractRequest entity representing a user's request for an insurance contract.
 * Maps to the contract_request table.
 */
public class ContractRequest {
    private int id;
    private int userId;
    private int assetId;
    private int packageId;
    private double calculatedPremium;
    private RequestStatus status;
    private LocalDateTime createdAt;
    private String boldSignDocumentId;   // BoldSign document ID, set after sending for signature

    public ContractRequest() {}
    public ContractRequest(int id, int userId, int assetId, int packageId,
                           double calculatedPremium, RequestStatus status, LocalDateTime createdAt) {
        this.id = id;
        this.userId = userId;
        this.assetId = assetId;
        this.packageId = packageId;
        this.calculatedPremium = calculatedPremium;
        this.status = status;
        this.createdAt = createdAt;
    }
    public ContractRequest(int userId, int assetId, int packageId,
                           double calculatedPremium, RequestStatus status) {
        this.userId = userId;
        this.assetId = assetId;
        this.packageId = packageId;
        this.calculatedPremium = calculatedPremium;
        this.status = status;
    }
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }
    public int getAssetId() { return assetId; }
    public void setAssetId(int assetId) { this.assetId = assetId; }
    public int getPackageId() { return packageId; }
    public void setPackageId(int packageId) { this.packageId = packageId; }
    public double getCalculatedPremium() { return calculatedPremium; }
    public void setCalculatedPremium(double calculatedPremium) { this.calculatedPremium = calculatedPremium; }
    public RequestStatus getStatus() { return status; }
    public void setStatus(RequestStatus status) { this.status = status; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public String getBoldSignDocumentId() { return boldSignDocumentId; }
    public void setBoldSignDocumentId(String boldSignDocumentId) { this.boldSignDocumentId = boldSignDocumentId; }
    @Override
    public String toString() {
        return "ContractRequest{" +
                "id=" + id +
                ", userId=" + userId +
                ", assetId=" + assetId +
                ", packageId=" + packageId +
                ", calculatedPremium=" + calculatedPremium +
                ", status=" + status +
                ", createdAt=" + createdAt +
                ", boldSignDocumentId='" + boldSignDocumentId + '\'' +
                '}';
    }
}
