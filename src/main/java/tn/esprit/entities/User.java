package tn.esprit.entities;

import java.time.LocalDateTime;

/**
 * User entity representing a user in the system.
 */
public class User {
    private int id;
    private String name;
    private String email;
    private String passwordHash;
    private int roleId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private boolean isVerified;
    private String phone;
    // New fields
    private String verificationCode;
    private boolean googleAccount;
    private LocalDateTime lastLogin;
    private boolean faceRegistered;

    // -- Constructors ----------------------------------------------------------

    public User() {}

    /** Full constructor (all columns). */
    public User(int id, String name, String email, String passwordHash, int roleId,
                LocalDateTime createdAt, LocalDateTime updatedAt, boolean isVerified,
                String phone, String verificationCode, boolean googleAccount,
                LocalDateTime lastLogin, boolean faceRegistered) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.passwordHash = passwordHash;
        this.roleId = roleId;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.isVerified = isVerified;
        this.phone = phone;
        this.verificationCode = verificationCode;
        this.googleAccount = googleAccount;
        this.lastLogin = lastLogin;
        this.faceRegistered = faceRegistered;
    }

    /**
     * Legacy constructor kept for backward compatibility
     * (id + original 8 fields, new fields default to safe values).
     */
    public User(int id, String name, String email, String passwordHash, int roleId,
                LocalDateTime createdAt, LocalDateTime updatedAt, boolean isVerified, String phone) {
        this(id, name, email, passwordHash, roleId, createdAt, updatedAt, isVerified,
             phone, null, false, null, false);
    }

    /** Creation constructor (no id / timestamps). */
    public User(String name, String email, String passwordHash, int roleId, String phone) {
        this.name = name;
        this.email = email;
        this.passwordHash = passwordHash;
        this.roleId = roleId;
        this.phone = phone;
    }

    // -- Getters & Setters -----------------------------------------------------

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }

    public int getRoleId() { return roleId; }
    public void setRoleId(int roleId) { this.roleId = roleId; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public boolean isVerified() { return isVerified; }
    public void setVerified(boolean verified) { isVerified = verified; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getVerificationCode() { return verificationCode; }
    public void setVerificationCode(String verificationCode) { this.verificationCode = verificationCode; }

    public boolean isGoogleAccount() { return googleAccount; }
    public void setGoogleAccount(boolean googleAccount) { this.googleAccount = googleAccount; }

    public LocalDateTime getLastLogin() { return lastLogin; }
    public void setLastLogin(LocalDateTime lastLogin) { this.lastLogin = lastLogin; }

    public boolean isFaceRegistered() { return faceRegistered; }
    public void setFaceRegistered(boolean faceRegistered) { this.faceRegistered = faceRegistered; }

    // -- toString --------------------------------------------------------------

    @Override
    public String toString() {
        return "User{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", email='" + email + '\'' +
                ", roleId=" + roleId +
                ", createdAt=" + createdAt +
                ", isVerified=" + isVerified +
                ", phone='" + phone + '\'' +
                ", verificationCode='" + verificationCode + '\'' +
                ", googleAccount=" + googleAccount +
                ", lastLogin=" + lastLogin +
                ", faceRegistered=" + faceRegistered +
                '}';
    }
}
