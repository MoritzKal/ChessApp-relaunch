package com.chessapp.api.training.service;

import java.util.Map;
import java.util.UUID;

public interface MlClient {
    void postTrain(UUID runId, UUID datasetId, Map<String, Object> params);
    Map<String, Object> getRun(UUID runId);
}
