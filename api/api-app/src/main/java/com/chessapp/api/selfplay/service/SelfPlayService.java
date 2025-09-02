package com.chessapp.api.selfplay.service;

import java.util.Map;
import java.util.UUID;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import com.chessapp.api.selfplay.api.dto.SelfPlayRunRequest;
import com.chessapp.api.selfplay.api.dto.SelfPlayRunResponse;

@Service
public class SelfPlayService {

    private final WebClient web;

    public SelfPlayService(WebClient selfPlayRunnerWebClient) {
        this.web = selfPlayRunnerWebClient;
    }

    public UUID start(SelfPlayRunRequest req, String idempotencyKey) {
        SelfPlayRunResponse resp = web.post().uri("/runner/selfplay/start")
                .headers(h -> {
                    if (idempotencyKey != null && !idempotencyKey.isEmpty()) {
                        h.add("Idempotency-Key", idempotencyKey);
                    }
                })
                .bodyValue(req)
                .retrieve()
                .bodyToMono(SelfPlayRunResponse.class)
                .block();
        return resp.runId();
    }

    public Map<String, Object> get(UUID runId) {
        return web.get().uri("/runner/selfplay/runs/{id}", runId)
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})
                .block();
    }
}
