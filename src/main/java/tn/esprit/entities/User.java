package tn.esprit.entities;

import java.time.LocalDateTime;

/**
 * User entity.
 *
 * Added fields vs original:
 *  - verificationCode   : 6-digit code emailed on registration
 *  - googleAccount      : true if created via Google OAuth
 *  - faceRegistered     : true if face model enrolled (OpenCV LBPH)
 *
 * DB columns to add (run once):
 *   ALTER TABLE user ADD COLUMN verification_code VARCHAR(10) DEFAULT NULL;
 *   ALTER TABLE user ADD COLUMN google_account    BOOLEAN     DEFAULT FALSE;
 *   ALTER TABLE user ADD COLUMN face_registered   BOOLEAN     DEFAULT FALSE;
 *   ALTER TABLE user ADD COLUMN last_login        DATETIME    DEFAULT NULL;
 */
public class User {

    private int           id;
    private String        name;
    private String        email;
    private String        passwordHash;
    private int           roleId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private boolean       isVerified;
    private String        phone;

    // ── new fields ────────────────────────────────────────────────────
    private String        verificationCode;
    private boolean       googleAccount;
    private boolean       faceRegistered;
    private LocalDateTime lastLogin;

    // ── constructors ──────────────────────────────────────────────────

    public User() {}

    /** Full constructor — used by UserDAO when reading from DB. */
    public User(int id, String name, String email, String passwordHash,
                int roleId, LocalDateTime createdAt, LocalDateTime updatedAt,
                boolean isVerified, String phone,
                String verificationCode, boolean googleAccount,
                boolean faceRegistered, LocalDateTime lastLogin) {
        this.id               = id;
        this.name             = name;
        this.email            = email;
        this.passwordHash     = passwordHash;
        this.roleId           = roleId;
        this.createdAt        = createdAt;
        this.updatedAt        = updatedAt;
        this.isVerified       = isVerified;
        this.phone            = phone;
        this.verificationCode = verificationCode;
        this.googleAccount    = googleAccount;
        this.faceRegistered   = faceRegistered;
        this.lastLogin        = lastLogin;
    }

    /** Convenience constructor — used when creating a new user (no id/timestamps yet). */
    public User(String name, String email, String passwordHash, int roleId, String phone) {
        this.name         = name;
        this.email        = email;
        this.passwordHash = passwordHash;
        this.roleId       = roleId;
        this.phone        = phone;
        this.isVerified   = false;
    }

    // ── getters & setters ─────────────────────────────────────────────

    public int getId()                          { return id; }
    public void setId(int id)                   { this.id = id; }

    public String getName()                     { return name; }
    public void setName(String name)            { this.name = name; }

    public String getEmail()                    { return email; }
    public void setEmail(String email)          { this.email = email; }

    public String getPasswordHash()             { return passwordHash; }
    public void setPasswordHash(String hash)    { this.passwordHash = hash; }

    public int getRoleId()                      { return roleId; }
    public void setRoleId(int roleId)           { this.roleId = roleId; }

    public LocalDateTime getCreatedAt()         { return createdAt; }
    public void setCreatedAt(LocalDateTime t)   { this.createdAt = t; }

    public LocalDateTime getUpdatedAt()         { return updatedAt; }
    public void setUpdatedAt(LocalDateTime t)   { this.updatedAt = t; }

    public boolean isVerified()                 { return isVerified; }
    public void setVerified(boolean verified)   { this.isVerified = verified; }

    public String getPhone()                    { return phone; }
    public void setPhone(String phone)          { this.phone = phone; }

    public String getVerificationCode()                      { return verificationCode; }
    public void setVerificationCode(String code)             { this.verificationCode = code; }

    public boolean isGoogleAccount()                         { return googleAccount; }
    public void setGoogleAccount(boolean googleAccount)      { this.googleAccount = googleAccount; }

    public boolean isFaceRegistered()                        { return faceRegistered; }
    public void setFaceRegistered(boolean faceRegistered)    { this.faceRegistered = faceRegistered; }

    public LocalDateTime getLastLogin()                      { return lastLogin; }
    public void setLastLogin(LocalDateTime lastLogin)        { this.lastLogin = lastLogin; }

    @Override
    public String toString() {
        return "User{id=" + id + ", name='" + name + "', email='" + email +
                "', roleId=" + roleId + ", verified=" + isVerified + "}";
    }
}