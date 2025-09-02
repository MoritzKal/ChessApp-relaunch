package com.chessapp.api.training.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Map;
import java.util.UUID;

@Component
public class MlClientWeb implements MlClient {

    private final WebClient web;

    public MlClientWeb(@Value("${chs.ml.base-url:http://ml:8000}") String baseUrl) {
        this.web = WebClient.builder().baseUrl(baseUrl).build();
    }

    @Override
    public void postTrain(UUID runId, UUID datasetId, Map<String, Object> params) {
        Map<String, Object> body = Map.of(
                "runId", runId.toString(),
                "datasetId", datasetId != null ? datasetId.toString() : null,
                "epochs", params != null && params.get("epochs") != null ? (Integer) params.get("epochs") : 10,
                "stepsPerEpoch", params != null && params.get("stepsPerEpoch") != null ? (Integer) params.get("stepsPerEpoch") : 50,
                "lr", params != null && params.get("lr") != null ? ((Number) params.get("lr")).doubleValue() : 1e-3
        );
        web.post().uri("/train")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(body)
                .retrieve()
                .bodyToMono(new org.springframework.core.ParameterizedTypeReference<java.util.Map<String,Object>>() {})
                .block();
    }

    @Override
    public Map<String, Object> getRun(UUID runId) {
        return web.get().uri("/runs/{id}", runId.toString())
                .retrieve()
                .bodyToMono(new org.springframework.core.ParameterizedTypeReference<java.util.Map<String,Object>>() {})
                .blockOptional()
                .orElse(Map.of("status","unknown"));
    }
}
