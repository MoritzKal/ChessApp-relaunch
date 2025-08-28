package com.chessapp.api.it;

import com.chessapp.api.codex.CodexApplication;
import com.chessapp.api.testutil.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;

import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(
        classes = CodexApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "logging.config=classpath:logback-spring.xml",
                "management.prometheus.metrics.export.enabled=true"
        }
)
@ActiveProfiles("codex")
class IngestIdempotencyIT extends AbstractIntegrationTest {

    @LocalServerPort
    int port;

    @Autowired
    TestRestTemplate rest;

    private String baseUrl() {
        return "http://localhost:" + port;
    }

    @Test
    @Timeout(90)
    void secondIngestIsSkippedAndMetricsRecorded() throws Exception {
        Map<String,Object> req = new HashMap<>();
        req.put("username", "M3NG00S3");
        req.put("from", "2025-07");
        req.put("to", "2025-08");
        req.put("offline", true);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        ResponseEntity<Map> start1 = rest.postForEntity(new URI(baseUrl() + "/v1/ingest"), new HttpEntity<>(req, headers), Map.class);
        assertThat(start1.getStatusCode().is2xxSuccessful() || start1.getStatusCode().is3xxRedirection()).isTrue();
        UUID run1 = UUID.fromString(String.valueOf(start1.getBody().get("runId")));
        waitUntilSucceeded(run1);

        ResponseEntity<Map> start2 = rest.postForEntity(new URI(baseUrl() + "/v1/ingest"), new HttpEntity<>(req, headers), Map.class);
        assertThat(start2.getStatusCode().is2xxSuccessful() || start2.getStatusCode().is3xxRedirection()).isTrue();
        UUID run2 = UUID.fromString(String.valueOf(start2.getBody().get("runId")));
        Map<String,Object> status2 = waitUntilSucceeded(run2);

        Object counts = status2.get("counts");
        assertThat(counts).isInstanceOf(Map.class);
        Object gamesCount = ((Map<?,?>) counts).get("games");
        assertThat(Integer.parseInt(String.valueOf(gamesCount))).isEqualTo(0);

        ResponseEntity<Map> metricResp = rest.exchange(baseUrl() + "/actuator/metrics/chs_ingest_skipped_total", HttpMethod.GET, null,
                new ParameterizedTypeReference<>() {});
        assertThat(metricResp.getStatusCode().is2xxSuccessful()).isTrue();
        List<Map<String,Object>> measurements = (List<Map<String,Object>>) metricResp.getBody().get("measurements");
        double val = Double.parseDouble(String.valueOf(measurements.get(0).get("value")));
        assertThat(val).isGreaterThan(0.0);

        ResponseEntity<String> promResp = rest.getForEntity(baseUrl() + "/actuator/prometheus", String.class);
        assertThat(promResp.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(promResp.getBody()).contains("chs_ingest_skipped_total");
    }

    private Map<String,Object> waitUntilSucceeded(UUID runId) throws Exception {
        String url = baseUrl() + "/v1/ingest/" + runId;
        Instant start = Instant.now();
        String status;
        Map<String,Object> body;
        do {
            ResponseEntity<Map> res = rest.getForEntity(url, Map.class);
            assertThat(res.getStatusCode().is2xxSuccessful()).isTrue();
            body = res.getBody();
            status = String.valueOf(body.get("status"));
            if ("failed".equalsIgnoreCase(status)) {
                throw new AssertionError("Ingest failed: " + body);
            }
            if ("succeeded".equalsIgnoreCase(status)) break;
            Thread.sleep(300);
        } while (Duration.between(start, Instant.now()).getSeconds() < 60);
        assertThat(status).isEqualTo("succeeded");
        return body;
    }
}

