package com.chessapp.api.selfplay.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Self-play run start request")
public record SelfPlayRunRequest(
        @Schema(example = "modelA") String modelId,
        @Schema(example = "baselineA") String baselineId,
        @Schema(example = "100") int games,
        @Schema(example = "4") int concurrency,
        @Schema(example = "42") Long seed
) {}
