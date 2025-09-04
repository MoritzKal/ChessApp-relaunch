package com.chessapp.api.ingest.api.dto;

/**
 * DTO representing current status of an ingest run.
 */
public record IngestStatusResponse(
        String runId,
        String status,
        String datasetId,
        String version,
        String message
) {}
