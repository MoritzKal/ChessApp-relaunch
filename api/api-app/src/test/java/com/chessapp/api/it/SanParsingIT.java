package com.chessapp.api.it;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.*;
import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.util.*;

import com.chessapp.api.domain.repo.MoveRepository;
import com.chessapp.api.domain.repo.PositionRepository;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        classes = com.chessapp.api.codex.CodexApplication.class,
        properties = {
                "logging.config=classpath:logback-spring.xml",
                "management.prometheus.metrics.export.enabled=true"
        })
@ActiveProfiles("codex")
class SanParsingIT extends com.chessapp.api.testutil.AbstractIntegrationTest {

  @Autowired TestRestTemplate rest;
  @Autowired MoveRepository moveRepo;
  @Autowired PositionRepository posRepo;

  @Test
  void san_is_set_and_positions_consistent() throws Exception {
    String runId = startIngest(rest);
    waitUntilSucceeded(rest, runId);

    var anyMove = moveRepo.findAll().stream().findFirst();
    assertThat(anyMove).isPresent();
    assertThat(anyMove.get().getSan()).isNotBlank();

    long moves = moveRepo.count();
    long positions = posRepo.count();
    assertThat(positions).isGreaterThanOrEqualTo(moves);

    ResponseEntity<Map> metric = rest.getForEntity("/actuator/metrics/chs_positions_legal_moves_total", Map.class);
    assertThat(metric.getStatusCode().is2xxSuccessful()).isTrue();
    var measurements = (List<Map<String,Object>>) metric.getBody().get("measurements");
    assertThat(measurements).isNotEmpty();
    Number val = (Number) measurements.get(0).get("value");
    assertThat(val.doubleValue()).isGreaterThan(0.0);
  }

  private static String startIngest(TestRestTemplate rest) {
    HttpHeaders h = new HttpHeaders(); h.setContentType(MediaType.APPLICATION_JSON);
    String body = """
      {"username":"M3NG00S3","from":"2025-07","to":"2025-08","offline":true}
    """;
    ResponseEntity<Map> resp = rest.postForEntity("/v1/ingest", new HttpEntity<>(body, h), Map.class);
    assertThat(resp.getStatusCode().value()).isEqualTo(202);
    return (String) resp.getBody().get("runId");
  }

  @SuppressWarnings("unchecked")
  private static void waitUntilSucceeded(TestRestTemplate rest, String runId) throws InterruptedException {
    for (int i = 0; i < 60; i++) {
      var r = rest.getForEntity("/v1/ingest/" + runId, Map.class);
      assertThat(r.getStatusCode().is2xxSuccessful()).isTrue();
      String status = (String) r.getBody().get("status");
      if ("succeeded".equalsIgnoreCase(status)) return;
      Thread.sleep(Duration.ofSeconds(2).toMillis());
    }
    throw new AssertionError("ingest did not reach succeeded in time");
  }
}
