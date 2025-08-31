package com.chessapp.api.models.api.dto;

import java.util.List;

public record ModelSummary(
        String modelId,
        String displayName,
        List<String> tags
) {}
