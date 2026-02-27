package tn.esprit.entities;

import java.time.LocalDateTime;

public class InsuredAsset {

    private int id;
    private String name;
    private String type;
    private double value;
    private String description;
    private LocalDateTime createdAt;
    private String location;
    private int userId;

    // Constructors
    public InsuredAsset() {}

    public InsuredAsset(int id, String name, String type, double value,
                        String description, LocalDateTime createdAt, String location, int userId) {
        this.id = id;
        this.name = name;
        this.type = type;
        this.value = value;
        this.description = description;
        this.createdAt = createdAt;
        this.location = location;
        this.userId = userId;
    }

    public InsuredAsset(String name, String type, double value,
                        String description, String location, int userId) {
        this.name = name;
        this.type = type;
        this.value = value;
        this.description = description;
        this.location = location;
        this.userId = userId;
    }

    // Getters & Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public double getValue() { return value; }
    public void setValue(double value) { this.value = value; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }

    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }


    @Override
    public String toString() {
        return "InsuredAsset{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", type='" + type + '\'' +
                ", value=" + value +
                ", description='" + description + '\'' +
                ", createdAt=" + createdAt +
                ", location='" + location + '\'' +
                ", userId=" + userId +
                '}';
    }
}
