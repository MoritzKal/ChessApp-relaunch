package com.chessapp.api.serving;

import java.util.HashMap;
import java.util.Map;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import com.chessapp.api.models.api.dto.ModelVersionSummary;
import com.chessapp.api.models.service.ModelRegistryService;
import com.chessapp.api.serving.dto.ModelsLoadRequest;
import com.chessapp.api.serving.dto.PredictRequest;
import com.chessapp.api.serving.dto.PredictResponse;

@Component
public class ServingClientWeb implements ServingClient {

    private final WebClient web;
    private final ModelRegistryService registry;

    public ServingClientWeb(WebClient servingWebClient, ModelRegistryService registry) {
        this.web = servingWebClient;
        this.registry = registry;
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
        // Upstream expects { modelId, modelVersion? } or may accept artifactUri; enrich with latest version if missing
        Map<String, Object> body = new HashMap<>();
        body.put("modelId", request.modelId());
        if (request.artifactUri() != null) {
            body.put("artifactUri", request.artifactUri());
        }
        try {
            // Resolve latest version for the modelId
            var versions = registry.listVersions(request.modelId());
            if (versions != null && !versions.isEmpty()) {
                ModelVersionSummary v = versions.get(0);
                if (v != null && v.modelVersion() != null && !v.modelVersion().isBlank()) {
                    body.put("modelVersion", v.modelVersion());
                }
            }
        } catch (Exception ignored) { /* fall back to body without version */ }

        return web.post().uri("/models/load")
                .header("X-Run-Id", runId)
                .header("X-Username", username)
                .header("X-Component", "serve")
                .bodyValue(body)
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})
                .block();
    }
}
