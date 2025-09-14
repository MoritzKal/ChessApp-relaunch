package com.chessapp.api.metrics.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Map;

@Component
public class ObsProxyClientWeb implements ObsProxyClient {

    private final WebClient web;

    public ObsProxyClientWeb(@Value("${chs.obs.base-url:http://obs-proxy:8088}") String baseUrl) {
        this.web = WebClient.builder().baseUrl(baseUrl).build();
    }

    @Override
    public Map<String, Object> promRange(String query, long start, long end, String step) {
        try {
            return web.get().uri(uriBuilder -> uriBuilder.path("/obs/prom/range")
                    .queryParam("query", query)
                    .queryParam("start", start)
                    .queryParam("end", end)
                    .queryParam("step", step)
                    .build())
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<Map<String,Object>>() {})
                    .block();
        } catch (Exception e) {
            return Map.of("data", Map.of("result", java.util.List.of()));
        }
    }

    @Override
    public Map<String, Object> lokiRange(String query, long start, long end, int limit, String direction) {
        try {
            return web.get().uri(uriBuilder -> uriBuilder.path("/obs/loki/query_range")
                    .queryParam("query", query)
                    .queryParam("start", start)
                    .queryParam("end", end)
                    .queryParam("limit", limit)
                    .queryParam("direction", direction)
                    .build())
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<Map<String,Object>>() {})
                    .block();
        } catch (Exception e) {
            return Map.of("data", Map.of("result", java.util.List.of()));
        }
    }
}
