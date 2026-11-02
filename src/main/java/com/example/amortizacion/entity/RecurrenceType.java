package com.example.amortizacion.entity;

public enum RecurrenceType {
    ONE_TIME("One-time"),
    MONTHLY("Monthly"),
    ANNUALLY("Annually");

    private final String displayName;

    RecurrenceType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}

