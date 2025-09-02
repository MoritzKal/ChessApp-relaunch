package com.chessapp.api.it;

import com.chessapp.api.support.JwtTestUtils;
import io.micrometer.core.instrument.MeterRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;

import java.time.Duration;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = com.chessapp.api.codex.CodexApplication.class)
@AutoConfigureMockMvc
@ActiveProfiles("codex")
class PrometheusTextExportIT extends com.chessapp.api.testutil.AbstractIntegrationTest {

  @Autowired MockMvc mvc;
  @Autowired MeterRegistry meterRegistry;
  @Value("${app.security.jwt.secret}") String secret;

  @Test
  void prometheus_text_export_is_available_and_labeled() throws Exception {
    meterRegistry.counter("chs_ingest_games_total", "username", "M3NG00S3").increment();

    var resp = mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/actuator/prometheus")
            .with(com.chessapp.api.testutil.TestAuth.jwtMonitoring()))
        .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isOk())
        .andReturn();
    String ct = resp.getResponse().getContentType();
    String token = JwtTestUtils.signHmac256(secret, Map.of(
        "sub", "mon",
        "roles", List.of("MONITORING")), Duration.ofMinutes(5));
    HttpHeaders headers = new HttpHeaders();
    headers.setBearerAuth(token);
    ResponseEntity<String> resp = rest.exchange(
        "/actuator/prometheus", HttpMethod.GET, new HttpEntity<>(headers), String.class);

    assertThat(resp.getStatusCode().value()).isEqualTo(200);
    ct = (ct == null) ? "" : ct;
    assertThat(ct.replace(" ", "")).startsWith("text/plain;version=0.0.4");
    String body = resp.getResponse().getContentAsString();
    assertThat(body).contains("chs_ingest_games_total");
    assertThat(body).contains("username=\"M3NG00S3\"");
  }
}
