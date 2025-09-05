package com.chessapp.api.datasets.api.dto;

import java.util.List;

public record DatasetDetailDto(
        String id,
        String name,
        long rows,
        long sizeBytes,
        int classes,
        String updatedAt,
        List<String> tags
) {}
