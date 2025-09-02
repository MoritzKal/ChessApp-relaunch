package com.chessapp.api.it;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import io.micrometer.core.instrument.MeterRegistry;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = com.chessapp.api.codex.CodexApplication.class)
@AutoConfigureMockMvc
@ActiveProfiles("codex")
class PrometheusTextExportIT extends com.chessapp.api.testutil.AbstractIntegrationTest {

  @Autowired MockMvc mvc;
  @Autowired MeterRegistry meterRegistry;

  @Test
  void prometheus_text_export_is_available_and_labeled() throws Exception {
    meterRegistry.counter("chs_ingest_games_total", "username", "M3NG00S3").increment();

    var resp = mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/actuator/prometheus")
            .with(com.chessapp.api.testutil.TestAuth.jwtMonitoring()))
        .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isOk())
        .andReturn();

    String ct = resp.getResponse().getContentType();
    ct = (ct == null) ? "" : ct;
    assertThat(ct.replace(" ", "")).startsWith("text/plain;version=0.0.4");
    String body = resp.getResponse().getContentAsString();
    assertThat(body).contains("chs_ingest_games_total");
    assertThat(body).contains("username=\"M3NG00S3\"");
  }
}
