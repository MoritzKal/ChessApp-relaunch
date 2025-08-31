package com.chessapp.api.ingest.api.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(name = "CreateIngestRequest", description = "Payload to start an ingest run")
@JsonInclude(JsonInclude.Include.NON_NULL)
public record CreateIngestRequest(
        @Schema(description = "Username initiating the ingest", example = "alice")
        @NotBlank String username,

        @Schema(description = "Optional range token", example = "2025-08")
        String range
) {}
