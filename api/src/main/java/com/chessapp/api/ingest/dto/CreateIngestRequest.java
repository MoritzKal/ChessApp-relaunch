package com.chessapp.api.ingest.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Start offline ingest", example = "{\"username\":\"alice\",\"range\":\"2025-08\"}")
public record CreateIngestRequest(
        @NotBlank
        @Schema(description = "Username initiating ingest", example = "alice")
        String username,

        @Schema(description = "Optional free-form range token", example = "2025-08")
        String range) {}
