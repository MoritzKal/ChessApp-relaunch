package com.chessapp.api.config;

import java.time.Duration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import reactor.netty.http.client.HttpClient;
import reactor.util.retry.Retry;

@Configuration
public class ServingClientConfig {

    @Bean
    public WebClient servingWebClient(@Value("${chs.serve.baseUrl:http://serve:8001}") String baseUrl) {
        HttpClient httpClient = HttpClient.create()
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 5000)
                .responseTimeout(Duration.ofSeconds(5))
                .doOnConnected(conn -> conn
                        .addHandlerLast(new ReadTimeoutHandler(5))
                        .addHandlerLast(new WriteTimeoutHandler(5)));

        Retry retrySpec = Retry.backoff(3, Duration.ofMillis(200))
                .filter(throwable -> throwable instanceof WebClientResponseException ex
                        && (ex.getStatusCode().is5xxServerError()
                            || ex.getStatusCode() == HttpStatus.TOO_MANY_REQUESTS))
                .jitter(0.5);

        return WebClient.builder()
                .baseUrl(baseUrl)
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .filter((request, next) -> next.exchange(request).retryWhen(retrySpec))
                .build();
    }
}
