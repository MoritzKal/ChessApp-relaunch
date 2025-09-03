package com.chessapp.api.eval.service;

import java.util.Map;
import java.util.UUID;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import com.chessapp.api.eval.api.dto.EvalStartRequest;
import com.chessapp.api.eval.api.dto.EvalStartResponse;

@Service
public class EvalService {

    private final WebClient web;

    public EvalService(WebClient evalOfflineWebClient) {
        this.web = evalOfflineWebClient;
    }

    public UUID start(EvalStartRequest req, String idempotencyKey) {
        EvalStartResponse resp = web.post().uri("/runner/eval/start")
                .headers(h -> {
                    if (idempotencyKey != null && !idempotencyKey.isEmpty()) {
                        h.add("Idempotency-Key", idempotencyKey);
                    }
                })
                .bodyValue(req)
                .retrieve()
                .bodyToMono(EvalStartResponse.class)
                .block();
        return resp.evalId();
    }

    public Map<String, Object> get(UUID evalId) {
        return web.get().uri("/runner/eval/{id}", evalId)
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})
                .block();
    }
}
