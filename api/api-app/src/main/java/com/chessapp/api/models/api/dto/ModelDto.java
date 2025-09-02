package com.chessapp.api.models.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Schema(description = "Model information")
public record ModelDto(
        @Schema(example = "d290f1ee-6c54-4b01-90e6-d701748f0851") UUID id,
        @Schema(example = "policy") String name,
        @Schema(example = "v1") String version,
        @Schema(example = "tensorflow") String framework,
        Map<String, Object> metrics,
        @Schema(example = "s3://bucket/model.tar") String artifactUri,
        @Schema(example = "false") boolean isProd,
        Instant createdAt
) {}
