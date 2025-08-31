package com.chessapp.api.ingest.api.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record CreateIngestRequest(String username, String range) {}
