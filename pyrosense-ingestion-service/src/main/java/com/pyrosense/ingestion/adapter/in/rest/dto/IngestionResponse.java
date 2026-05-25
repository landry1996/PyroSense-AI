package com.pyrosense.ingestion.adapter.in.rest.dto;

public record IngestionResponse(String status, String message) {

    public static IngestionResponse accepted() {
        return new IngestionResponse("ACCEPTED", "Telemetry ingested successfully");
    }

    public static IngestionResponse duplicate() {
        return new IngestionResponse("DUPLICATE", "Duplicate reading ignored");
    }

    public static IngestionResponse rejected(String reason) {
        return new IngestionResponse("REJECTED", reason);
    }
}
