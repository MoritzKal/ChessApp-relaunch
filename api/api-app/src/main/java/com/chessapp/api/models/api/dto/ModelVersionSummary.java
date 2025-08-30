package com.chessapp.api.models.api.dto;

import java.time.Instant;
import java.util.Map;

public record ModelVersionSummary(
        String modelVersion,
        Instant createdAt,
        Map<String, Object> metrics
) {}
