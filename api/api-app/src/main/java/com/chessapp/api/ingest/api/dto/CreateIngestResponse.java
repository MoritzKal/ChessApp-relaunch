package com.chessapp.api.ingest.api.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

@Schema(name = "CreateIngestResponse", description = "Identifier of the started ingest run")
@JsonInclude(JsonInclude.Include.NON_NULL)
public record CreateIngestResponse(
        @Schema(description = "Run identifier", example = "123e4567-e89b-12d3-a456-426614174000")
        UUID runId
) {}
