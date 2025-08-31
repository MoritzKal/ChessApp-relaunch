package com.chessapp.api.security;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpHeaders;
import org.springframework.web.reactive.function.client.WebClient;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ActuatorSecurityIT {
  @LocalServerPort int port;
  @Value("${security.monitoring.token}") String mon;

  @Test
  void prometheus_requires_token() {
    int code = WebClient.create("http://localhost:"+port)
      .get().uri("/actuator/prometheus").exchangeToMono(r -> r.toBodilessEntity().map(e -> e.getStatusCode().value()))
      .block();
    assertThat(code).isEqualTo(401);
  }

  @Test
  void prometheus_with_token_ok() {
    String body = WebClient.create("http://localhost:"+port)
      .get().uri("/actuator/prometheus")
      .header(HttpHeaders.AUTHORIZATION, "Bearer " + mon)
      .retrieve().bodyToMono(String.class).block();
    assertThat(body).contains("http_server_requests_seconds_bucket");
  }
}

