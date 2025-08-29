package com.chessapp.api.training.api;

import java.util.Map;
import java.util.UUID;

public record TrainingStartRequest(
        UUID datasetId,
        String preset,
        Map<String, Object> params
) {}
