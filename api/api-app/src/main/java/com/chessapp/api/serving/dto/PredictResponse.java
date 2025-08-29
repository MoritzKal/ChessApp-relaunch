package com.chessapp.api.serving.dto;

import java.util.List;

public record PredictResponse(String move, List<String> legal, String modelId, String modelVersion) {}
