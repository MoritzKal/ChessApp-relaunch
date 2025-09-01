package com.chessapp.api.ingest.api.dto;

import java.util.UUID;

/**
 * Response returned when an ingest run is started.
 */
public record IngestStartResponse(UUID runId) {
}
