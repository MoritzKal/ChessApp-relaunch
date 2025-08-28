package com.chessapp.api.it;

import com.chessapp.api.ingest.service.ArtifactWriter;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.*;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.time.Duration;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        classes = com.chessapp.api.codex.CodexApplication.class)
@ActiveProfiles("codex")
class IngestReportUriIT extends com.chessapp.api.testutil.AbstractIntegrationTest {

  @Autowired TestRestTemplate rest;

  @MockBean ArtifactWriter artifactWriter;

  @Test
  void offline_ingest_writes_report_and_exposes_reportUri() throws Exception {
    when(artifactWriter.putReport(anyString(), any()))
        .thenAnswer(inv -> "s3://reports/ingest/" + inv.getArgument(0, String.class) + "/report.json");

    String runId = startIngest(rest);

    Map<String,Object> status = waitUntilSucceeded(rest, runId);
    String reportUri = (String) status.get("reportUri");
    assertThat(reportUri).isEqualTo("s3://reports/ingest/" + runId + "/report.json");

    verify(artifactWriter, atLeastOnce()).putReport(eq(runId), any());
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
