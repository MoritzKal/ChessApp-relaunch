package com.chessapp.api.ingest;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Ingest run status")
public enum IngestRunStatus {
    PENDING,
    RUNNING,
    SUCCEEDED,
    FAILED
}
