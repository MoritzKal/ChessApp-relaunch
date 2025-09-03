package com.chessapp.api.eval.api.dto;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Evaluation start request")
public record EvalStartRequest(
        @Schema(example = "modelA") String modelId,
        @Schema(example = "datasetA") String datasetId,
        @Schema(example = "[\"accuracy\",\"loss\"]") List<String> metrics
) {}
