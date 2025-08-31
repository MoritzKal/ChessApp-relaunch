package com.chessapp.api.ingest.api.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "IngestStatusResponse", description = "Status information for an ingest run")
@JsonInclude(JsonInclude.Include.NON_NULL)
public record IngestStatusResponse(
        @Schema(description = "Run status", example = "SUCCEEDED", allowableValues = {"PENDING", "RUNNING", "SUCCEEDED", "FAILED"})
        String status,

        @Schema(description = "Report location", example = "s3://reports/ingest/123e4567-e89b-12d3-a456-426614174000/report.json")
        String reportUri
) {}
