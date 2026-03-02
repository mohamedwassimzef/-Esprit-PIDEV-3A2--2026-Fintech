package tn.esprit.entities;

/**
 * Role entity representing a user role in the system.
 */
public class Role {
    private int id;
    private String roleName;
    private String permissions; // JSON format

    // Constructors
    public Role() {}

    public Role(int id, String roleName, String permissions) {
        this.id = id;
        this.roleName = roleName;
        this.permissions = permissions;
    }

    public Role(String roleName, String permissions) {
        this.roleName = roleName;
        this.permissions = permissions;
    }

    // Getters & Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getRoleName() { return roleName; }
    public void setRoleName(String roleName) { this.roleName = roleName; }

    public String getPermissions() { return permissions; }
    public void setPermissions(String permissions) { this.permissions = permissions; }

    @Override
    public String toString() {
        return "Role{" +
                "id=" + id +
                ", roleName='" + roleName + '\'' +
                ", permissions='" + permissions + '\'' +
                '}';
    }
}

