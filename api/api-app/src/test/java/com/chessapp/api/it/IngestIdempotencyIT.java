package com.chessapp.api.it;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.*;
import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.util.Map;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        classes = com.chessapp.api.codex.CodexApplication.class,
        properties = {
                "logging.config=classpath:logback-spring.xml",
                "management.prometheus.metrics.export.enabled=true"
        })
@ActiveProfiles("codex")
class IngestIdempotencyIT extends com.chessapp.api.testutil.AbstractIntegrationTest {

  @Autowired TestRestTemplate rest;

  @Test
  void second_offline_ingest_skips_duplicates_and_increments_metric() throws Exception {
    // --- Run 1 ---
    String runId1 = startIngest(rest);
    waitUntilSucceeded(rest, runId1);

    // --- Run 2 (same input) ---
    String runId2 = startIngest(rest);
    Map<String,Object> stat2 = waitUntilSucceeded(rest, runId2);

    // Expect: zero new games
    Map<String, Object> counts2 = (Map<String, Object>) stat2.get("counts");
    Number games2 = (Number) counts2.get("games");
    assertThat(games2.intValue()).isEqualTo(0);

    // Metrics JSON
    ResponseEntity<Map> metric = rest.getForEntity("/actuator/metrics/chs_ingest_skipped_total", Map.class);
    assertThat(metric.getStatusCode().is2xxSuccessful()).isTrue();
    var measurements = (java.util.List<Map<String,Object>>) metric.getBody().get("measurements");
    assertThat(measurements).isNotEmpty();
    Number val = (Number) measurements.get(0).get("value");
    assertThat(val.doubleValue()).isGreaterThan(0.0);

    // Prometheus text (best effort)
    ResponseEntity<String> prom = rest.getForEntity("/actuator/prometheus", String.class);
    assertThat(prom.getStatusCode().is2xxSuccessful()).isTrue();
    assertThat(prom.getBody()).contains("chs_ingest_skipped_total");
    assertThat(prom.getBody()).contains("username=\"M3NG00S3\"");
  }

  private static String startIngest(TestRestTemplate rest) {
    HttpHeaders h = new HttpHeaders();
    h.setContentType(MediaType.APPLICATION_JSON);
    String body = """
      {"username":"M3NG00S3","from":"2025-07","to":"2025-08","offline":true}
    """;
    ResponseEntity<Map> resp = rest.postForEntity("/v1/ingest", new HttpEntity<>(body, h), Map.class);
    assertThat(resp.getStatusCode().value()).isEqualTo(202);
    return (String) resp.getBody().get("runId");
  }

  @SuppressWarnings("unchecked")
  private static Map<String,Object> waitUntilSucceeded(TestRestTemplate rest, String runId) throws InterruptedException {
    for (int i = 0; i < 60; i++) {
      ResponseEntity<Map> r = rest.getForEntity("/v1/ingest/" + runId, Map.class);
      assertThat(r.getStatusCode().is2xxSuccessful()).isTrue();
      Map<String,Object> body = r.getBody();
      String status = (String) body.get("status");
      if ("succeeded".equalsIgnoreCase(status)) {
        return body;
      }
      Thread.sleep(Duration.ofSeconds(2).toMillis());
    }
    throw new AssertionError("ingest did not reach succeeded in time");
  }
}
