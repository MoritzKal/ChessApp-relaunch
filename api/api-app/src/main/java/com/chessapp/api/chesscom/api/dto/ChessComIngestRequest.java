package com.chessapp.api.chesscom.api.dto;

import java.util.List;

public record ChessComIngestRequest(String user, List<String> months, String datasetId, String note) {}
