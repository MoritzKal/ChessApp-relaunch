package com.chessapp.api.ingest.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Ingest run status response")
public record IngestStatusResponse(
        @Schema(description = "Current run status", example = "SUCCEEDED")
        String status,

        @Schema(description = "S3 report location if available", example = "s3://reports/ingest/<runId>/report.json")
        String reportUri) {}
