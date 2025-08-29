package com.chessapp.api.serving;

import java.util.Map;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import com.chessapp.api.serving.dto.ModelsLoadRequest;
import com.chessapp.api.serving.dto.PredictRequest;
import com.chessapp.api.serving.dto.PredictResponse;

@Component
public class ServingClientWeb implements ServingClient {

    private final WebClient web;

    public ServingClientWeb(WebClient servingWebClient) {
        this.web = servingWebClient;
    }

    @Override
    public PredictResponse predict(PredictRequest request, String runId, String username) {
        return web.post().uri("/predict")
                .header("X-Run-Id", runId)
                .header("X-Username", username)
                .header("X-Component", "serve")
                .bodyValue(request)
                .retrieve()
                .bodyToMono(PredictResponse.class)
                .block();
    }

    @Override
    public Map<String, Object> modelsLoad(ModelsLoadRequest request, String runId, String username) {
        return web.post().uri("/models/load")
                .header("X-Run-Id", runId)
                .header("X-Username", username)
                .header("X-Component", "serve")
                .bodyValue(request)
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})
                .block();
    }
}
