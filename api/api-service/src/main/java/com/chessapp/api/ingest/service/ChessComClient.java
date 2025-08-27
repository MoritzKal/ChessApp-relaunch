package com.chessapp.api.ingest.service;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Component
public class ChessComClient {

    private final WebClient webClient;

    public ChessComClient(WebClient.Builder builder) {
        this.webClient = builder.baseUrl("https://api.chess.com").build();
    }

    public Mono<List<String>> listArchives(String username) {
        return webClient.get()
                .uri("/pub/player/{username}/games/archives", username)
                .retrieve()
                .bodyToMono(JsonNode.class)
                .map(json -> {
                    List<String> archives = new ArrayList<>();
                    JsonNode arr = json.get("archives");
                    if (arr != null && arr.isArray()) {
                        arr.forEach(node -> archives.add(node.asText()));
                    }
                    return archives;
                });
    }

    public Flux<JsonNode> fetchMonth(String username, String yyyy, String mm) {
        return webClient.get()
                .uri("/pub/player/{username}/games/{yyyy}/{mm}", username, yyyy, mm)
                .retrieve()
                .bodyToMono(JsonNode.class)
                .flatMapMany(json -> {
                    JsonNode games = json.get("games");
                    if (games != null && games.isArray()) {
                        return Flux.fromIterable(games);
                    }
                    return Flux.empty();
                });
    }
}
