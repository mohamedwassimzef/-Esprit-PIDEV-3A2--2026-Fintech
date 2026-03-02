package tn.esprit.entities;
import java.time.LocalDateTime;
/**
 * InsurancePackage entity representing an insurance package in the system.
 * Maps to the insurance_package table.
 */
public class InsurancePackage {
    private int id;
    private String name;
    private String assetType;
    private String description;
    private String coverageDetails;
    private double basePrice;
    private double riskMultiplier;
    private int durationMonths;
    private boolean isActive;
    private LocalDateTime createdAt;
    public InsurancePackage() {}
    public InsurancePackage(int id, String name, String assetType, String description,
                            String coverageDetails, double basePrice, double riskMultiplier,
                            int durationMonths, boolean isActive, LocalDateTime createdAt) {
        this.id = id;
        this.name = name;
        this.assetType = assetType;
        this.description = description;
        this.coverageDetails = coverageDetails;
        this.basePrice = basePrice;
        this.riskMultiplier = riskMultiplier;
        this.durationMonths = durationMonths;
        this.isActive = isActive;
        this.createdAt = createdAt;
    }
    public InsurancePackage(String name, String assetType, String description,
                            String coverageDetails, double basePrice, double riskMultiplier,
                            int durationMonths, boolean isActive) {
        this.name = name;
        this.assetType = assetType;
        this.description = description;
        this.coverageDetails = coverageDetails;
        this.basePrice = basePrice;
        this.riskMultiplier = riskMultiplier;
        this.durationMonths = durationMonths;
        this.isActive = isActive;
    }
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getAssetType() { return assetType; }
    public void setAssetType(String assetType) { this.assetType = assetType; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getCoverageDetails() { return coverageDetails; }
    public void setCoverageDetails(String coverageDetails) { this.coverageDetails = coverageDetails; }
    public double getBasePrice() { return basePrice; }
    public void setBasePrice(double basePrice) { this.basePrice = basePrice; }
    public double getRiskMultiplier() { return riskMultiplier; }
    public void setRiskMultiplier(double riskMultiplier) { this.riskMultiplier = riskMultiplier; }
    public int getDurationMonths() { return durationMonths; }
    public void setDurationMonths(int durationMonths) { this.durationMonths = durationMonths; }
    public boolean isActive() { return isActive; }
    public void setActive(boolean active) { isActive = active; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    @Override
    public String toString() {
        return "InsurancePackage{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", assetType='" + assetType + '\'' +
                ", description='" + description + '\'' +
                ", coverageDetails='" + coverageDetails + '\'' +
                ", basePrice=" + basePrice +
                ", riskMultiplier=" + riskMultiplier +
                ", durationMonths=" + durationMonths +
                ", isActive=" + isActive +
                ", createdAt=" + createdAt +
                '}';
    }
}
