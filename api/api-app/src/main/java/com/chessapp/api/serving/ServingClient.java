package com.chessapp.api.serving;

import java.util.Map;

import com.chessapp.api.serving.dto.ModelsLoadRequest;
import com.chessapp.api.serving.dto.PredictRequest;
import com.chessapp.api.serving.dto.PredictResponse;

public interface ServingClient {
    PredictResponse predict(PredictRequest request, String runId, String username);
    Map<String, Object> modelsLoad(ModelsLoadRequest request, String runId, String username);
}
