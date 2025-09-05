package com.chessapp.api.chesscom.service;

import java.time.Duration;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import reactor.netty.http.client.HttpClient;
import reactor.util.retry.Retry;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;

@Service
public class ChessComService {

    private final WebClient web;
    private final Map<String, Long> nextAllowed = new ConcurrentHashMap<>();

    public ChessComService(@Value("${chess.ingest.baseUrl:https://api.chess.com}") String baseUrl) {
        HttpClient httpClient = HttpClient.create()
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 5000)
                .responseTimeout(Duration.ofSeconds(15))
                .doOnConnected(conn -> conn
                        .addHandlerLast(new ReadTimeoutHandler(15))
                        .addHandlerLast(new WriteTimeoutHandler(15)));

        Retry retrySpec = Retry.max(2)
                .filter(t -> t instanceof WebClientResponseException ex && ex.getStatusCode().is5xxServerError());

        ExchangeStrategies strategies = ExchangeStrategies.builder()
                .codecs(cfg -> cfg.defaultCodecs().maxInMemorySize(10 * 1024 * 1024)) // allow large PGN responses
                .build();

        this.web = WebClient.builder()
                .baseUrl(baseUrl)
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .exchangeStrategies(strategies)
                .filter((request, next) -> next.exchange(request).retryWhen(retrySpec))
                .build();
    }

    private void throttle(String user) {
        user = user.toLowerCase();
        long now = System.currentTimeMillis();
        long allowed = nextAllowed.getOrDefault(user, 0L);
        if (now < allowed) {
            try { Thread.sleep(allowed - now); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        }
        nextAllowed.put(user, System.currentTimeMillis() + 1000);
    }

    public List<String> listArchives(String user) {
        throttle(user);
        ArchivesWrapper wrapper = web.get()
                .uri("/pub/player/{user}/games/archives", user)
                .retrieve()
                .bodyToMono(ArchivesWrapper.class)
                .block();
        return wrapper.archives().stream()
                .map(url -> {
                    String[] parts = url.split("/");
                    String year = parts[parts.length - 2];
                    String month = parts[parts.length - 1];
                    return year + "-" + month;
                })
                .collect(Collectors.toList());
    }

    public ArchiveMeta meta(String user, int year, int month) {
        throttle(user);
        GamesWrapper wrapper = web.get()
                .uri("/pub/player/{user}/games/{year}/{month}", user, year, String.format("%02d", month))
                .retrieve()
                .bodyToMono(GamesWrapper.class)
                .block();
        int count = wrapper.games() == null ? 0 : wrapper.games().size();
        Map<String, Integer> dist = wrapper.games() == null ? Map.of() :
                wrapper.games().stream()
                        .collect(Collectors.groupingBy(Game::time_class, Collectors.summingInt(g -> 1)));
        return new ArchiveMeta(count, dist);
    }

    public byte[] downloadPgn(String user, YearMonth ym) {
        throttle(user);
        return web.get()
                .uri("/pub/player/{user}/games/{year}/{month}/pgn", user, ym.getYear(), String.format("%02d", ym.getMonthValue()))
                .retrieve()
                .bodyToMono(byte[].class)
                .block();
    }

    private record ArchivesWrapper(List<String> archives) {}
    private record GamesWrapper(List<Game> games) {}
    private record Game(String time_class) {}

    public record ArchiveMeta(int count, Map<String, Integer> timeControlDist) {}
}
