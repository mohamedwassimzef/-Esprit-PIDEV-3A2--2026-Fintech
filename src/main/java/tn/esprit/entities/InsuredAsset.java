package tn.esprit.entities;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public class InsuredAsset {

    private int id;
    private String reference;
    private String type;
    private String description;
    private LocalDateTime createdAt;
    private String location;
    private int userId;
    private BigDecimal declaredValue;
    private BigDecimal approvedValue;   // nullable
    private LocalDate manufactureDate;

    // ── Constructors ─────────────────────────────────────────────────────

    public InsuredAsset() {}

    /** Full constructor (used when reading from DB) */
    public InsuredAsset(int id, String reference, String type,
                        String description, LocalDateTime createdAt, String location,
                        int userId, BigDecimal declaredValue, BigDecimal approvedValue,
                        LocalDate manufactureDate) {
        this.id = id;
        this.reference = reference;
        this.type = type;
        this.description = description;
        this.createdAt = createdAt;
        this.location = location;
        this.userId = userId;
        this.declaredValue = declaredValue;
        this.approvedValue = approvedValue;
        this.manufactureDate = manufactureDate;
    }

    /** Create constructor (no id / createdAt / approvedValue yet) */
    public InsuredAsset(String reference, String type,
                        String description, String location, int userId,
                        BigDecimal declaredValue, LocalDate manufactureDate) {
        this.reference = reference;
        this.type = type;
        this.description = description;
        this.location = location;
        this.userId = userId;
        this.declaredValue = declaredValue;
        this.manufactureDate = manufactureDate;
    }

    // ── Getters & Setters ─────────────────────────────────────────────────

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getReference() { return reference; }
    public void setReference(String reference) { this.reference = reference; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }


    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }

    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }

    public BigDecimal getDeclaredValue() { return declaredValue; }
    public void setDeclaredValue(BigDecimal declaredValue) { this.declaredValue = declaredValue; }

    public BigDecimal getApprovedValue() { return approvedValue; }
    public void setApprovedValue(BigDecimal approvedValue) { this.approvedValue = approvedValue; }

    public LocalDate getManufactureDate() { return manufactureDate; }
    public void setManufactureDate(LocalDate manufactureDate) { this.manufactureDate = manufactureDate; }

    @Override
    public String toString() {
        return "InsuredAsset{" +
                "id=" + id +
                ", reference='" + reference + '\'' +
                ", type='" + type + '\'' +
                ", declaredValue=" + declaredValue +
                ", approvedValue=" + approvedValue +
                ", manufactureDate=" + manufactureDate +
                ", description='" + description + '\'' +
                ", createdAt=" + createdAt +
                ", location='" + location + '\'' +
                ", userId=" + userId +
                '}';
    }
}
