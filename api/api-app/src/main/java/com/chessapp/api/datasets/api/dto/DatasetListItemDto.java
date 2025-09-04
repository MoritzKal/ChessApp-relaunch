package com.chessapp.api.datasets.api.dto;

public record DatasetListItemDto(
        String id,
        String name,
        long rows,
        long sizeBytes,
        VersionsSummaryDto versions,
        String updatedAt
) {}
