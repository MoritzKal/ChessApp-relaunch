package com.chessapp.api.ingest.api.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.NotBlank;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record CreateIngestRequest(@NotBlank String username, String range) {}
