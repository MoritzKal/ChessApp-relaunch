package com.chessapp.api.training.api.dto;

/**
 * Summary of a training run returned in list endpoints.
 */
public record TrainingItemDto(String runId, String status, double progress, String updatedAt) {}
