package com.chessapp.api.training.api;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

/**
 * Request payload for starting a new training run.
 */
public record TrainingStartRequest(
        @NotNull UUID datasetId,
        @NotBlank String datasetVersion,
        @NotNull UUID modelId,
        @Min(1) int epochs,
        @Min(1) int batchSize,
        @DecimalMin(value = "0.0", inclusive = false) double learningRate,
        @NotBlank String optimizer,
        @NotNull Integer seed,
        String notes,
        boolean useGPU,
        @NotBlank String priority
) {}
