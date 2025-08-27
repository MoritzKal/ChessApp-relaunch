package com.chessapp.api.ingest.service;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

@Component
public class ChessComClient {

    private final WebClient client;

    public ChessComClient(WebClient.Builder builder,
                          @Value("${chess.ingest.baseUrl:https://api.chess.com}") String baseUrl) {
        this.client = builder.baseUrl(baseUrl).build();
    }

    public Mono<List<String>> listArchives(String username) {
        return client.get()
                .uri("/pub/player/{u}/games/archives", username)
                .retrieve()
                .onStatus(HttpStatusCode::isError, r -> r.createException().flatMap(Mono::error))
                .bodyToMono(JsonNode.class)
                .map(n -> {
                    JsonNode arr = n.get("archives");
                    List<String> out = new ArrayList<>();
                    if (arr != null && arr.isArray()) {
                        for (JsonNode x : arr) out.add(x.asText());
                    }
                    return out;
                })
                .retryWhen(Retry.backoff(5, Duration.ofMillis(300)).maxBackoff(Duration.ofSeconds(3)).jitter(0.5))
                .timeout(Duration.ofSeconds(10));
    }

    public Flux<JsonNode> fetchMonth(String username, YearMonth ym) {
        return client.get()
                .uri("/pub/player/{u}/games/{y}/{m}", username, ym.getYear(), String.format("%02d", ym.getMonthValue()))
                .retrieve()
                .onStatus(HttpStatusCode::isError, r -> r.createException().flatMap(Mono::error))
                .bodyToMono(JsonNode.class)
                .flatMapMany(n -> {
                    JsonNode arr = n.get("games");
                    if (arr == null || !arr.isArray()) return Flux.empty();
                    return Flux.fromIterable(arr);
                })
                .retryWhen(Retry.backoff(5, Duration.ofMillis(300)).maxBackoff(Duration.ofSeconds(3)).jitter(0.5))
                .timeout(Duration.ofSeconds(15));
    }
}
