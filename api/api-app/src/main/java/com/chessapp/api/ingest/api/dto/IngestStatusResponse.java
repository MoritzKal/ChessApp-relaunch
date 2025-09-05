package com.chessapp.api.ingest.api.dto;

/**
 * DTO representing current status of an ingest run.
 */
public record IngestStatusResponse(
        String runId,
        String status,
        String datasetId,
        java.util.List<String> versions,
        String reportUri,
        Long filesWritten
) {}
