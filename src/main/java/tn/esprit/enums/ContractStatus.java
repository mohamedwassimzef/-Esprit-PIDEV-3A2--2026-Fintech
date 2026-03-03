package tn.esprit.enums;

public enum ContractStatus {
    NOT_SIGNED,
    SIGNED,
    REJECTED;

    // Convert DB string → enum
    public static ContractStatus fromString(String value) {
        return ContractStatus.valueOf(value.toUpperCase());
    }

    // Convert enum → DB string
    public String toDbValue() {
        return this.name().toLowerCase();
    }
}
