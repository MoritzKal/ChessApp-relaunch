package com.chessapp.api.ingest.api.dto;

public record IngestRequest(String username, String from, String to, Boolean offline) {}
