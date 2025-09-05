package com.chessapp.api.datasets.api.dto;

import java.util.List;

public record SummaryDto(String id, long rows, long sizeBytes, int classes,
                         List<String> versions, String updatedAt) {}
