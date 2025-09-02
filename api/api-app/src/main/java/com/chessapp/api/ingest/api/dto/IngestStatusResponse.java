package com.chessapp.api.ingest.api.dto;

/**
 * DTO representing current status of an ingest run.
 */
public record IngestStatusResponse(String status, String reportUri) {
}
