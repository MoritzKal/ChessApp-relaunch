package com.chessapp.api.ingest.api.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.UUID;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record CreateIngestResponse(UUID runId) {}
