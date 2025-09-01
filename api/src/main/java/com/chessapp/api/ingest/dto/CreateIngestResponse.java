package com.chessapp.api.ingest.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.UUID;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Ingest run identifier")
public record CreateIngestResponse(
        @Schema(example = "123e4567-e89b-12d3-a456-426614174000")
        UUID runId) {}
