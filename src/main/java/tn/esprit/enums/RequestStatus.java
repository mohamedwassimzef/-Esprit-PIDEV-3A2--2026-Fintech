package tn.esprit.enums;

public enum RequestStatus {
    PENDING,
    APPROVED,
    REJECTED,
    SIGNED;

    // Convert DB string to enum
    public static RequestStatus fromString(String value) {
        return RequestStatus.valueOf(value.toUpperCase());
    }

    // Convert enum to DB string
    public String toDbValue() {
        return this.name().toUpperCase();
    }
}

