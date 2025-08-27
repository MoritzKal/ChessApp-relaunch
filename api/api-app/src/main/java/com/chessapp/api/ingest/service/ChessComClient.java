package com.chessapp.api.ingest.service;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.YearMonth;
import java.util.List;
import java.util.stream.Collectors;

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
            .onStatus(HttpStatus::isError, r -> r.createException().flatMap(Mono::error))
            .bodyToMono(JsonNode.class)
            .map(n -> {
                var arr = n.get("archives");
                if (arr == null || !arr.isArray()) return List.<String>of();
                return Flux.fromIterable(arr)
                        .map(JsonNode::asText)
                        .collectList()
                        .block();
            })
            .retryBackoff(5, Duration.ofMillis(300), Duration.ofSeconds(3))
            .timeout(Duration.ofSeconds(10));
    }

    public Flux<JsonNode> fetchMonth(String username, YearMonth ym) {
        return client.get()
            .uri("/pub/player/{u}/games/{y}/{m}", username, ym.getYear(), String.format("%02d", ym.getMonthValue()))
            .retrieve()
            .onStatus(HttpStatus::isError, r -> r.createException().flatMap(Mono::error))
            .bodyToMono(JsonNode.class)
            .flatMapMany(n -> {
                var arr = n.get("games");
                if (arr == null || !arr.isArray()) return Flux.empty();
                return Flux.fromIterable(arr);
            })
            .retryBackoff(5, Duration.ofMillis(300), Duration.ofSeconds(3))
            .timeout(Duration.ofSeconds(15));
    }
}
