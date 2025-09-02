package com.chessapp.api.it;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import io.micrometer.core.instrument.MeterRegistry;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    classes = com.chessapp.api.codex.CodexApplication.class)
@ActiveProfiles("codex")
class PrometheusTextExportIT extends com.chessapp.api.testutil.AbstractIntegrationTest {

  @Autowired TestRestTemplate rest;
  @Autowired MeterRegistry meterRegistry;

  @Test
  void prometheus_text_export_is_available_and_labeled() {
    meterRegistry.counter("chs_ingest_games_total", "username", "M3NG00S3").increment();

    ResponseEntity<String> resp = rest.getForEntity("/actuator/prometheus", String.class);

    assertThat(resp.getStatusCode().value()).isEqualTo(200);
    String ct = resp.getHeaders().getFirst("Content-Type");
    // Normalize to avoid potential null pointer in static analysis
    ct = (ct == null) ? "" : ct;
    // Spring Boot may emit with/without space before version and include charset
    assertThat(ct.replace(" ", "")).startsWith("text/plain;version=0.0.4");
    assertThat(resp.getBody()).contains("chs_ingest_games_total");
    assertThat(resp.getBody()).contains("username=\"M3NG00S3\"");
  }
}
