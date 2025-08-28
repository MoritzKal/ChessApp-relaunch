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
                // Avoid failing logback-codex.xml in tests
                "logging.config=classpath:logback-spring.xml",
                // Ensure Prometheus export is enabled in tests
                "management.prometheus.metrics.export.enabled=true"
        }
)
@ActiveProfiles("codex")
class IngestAndGamesFlowIT extends AbstractIntegrationTest {

    @LocalServerPort
    int port;

    @Autowired
    TestRestTemplate rest;

    private String baseUrl() {
        return "http://localhost:" + port;
    }

    @Test
    @Timeout(90)
    void flow_offlineIngest_list_detail_prometheus() throws Exception {
        // 1) Start ingest (offline=true) with from/to months
        String startUrl = baseUrl() + "/v1/ingest";
        Map<String,Object> req = new HashMap<>();
        req.put("username", "M3NG00S3");
        req.put("from", "2025-07");
        req.put("to", "2025-08");
        req.put("offline", true);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        ResponseEntity<Map> startResp = rest.postForEntity(new URI(startUrl), new HttpEntity<>(req, headers), Map.class);
        assertThat(startResp.getStatusCode().is2xxSuccessful() || startResp.getStatusCode().is3xxRedirection())
                .as("POST /v1/ingest should be accepted").isTrue();
        Object runIdObj = startResp.getBody().get("runId");
        assertThat(runIdObj).as("runId present").isInstanceOf(String.class);
        UUID runId = UUID.fromString((String) runIdObj);

        // 2) Poll status until succeeded (or timeout)
        String statusUrl = baseUrl() + "/v1/ingest/" + runId;
        Instant start = Instant.now();
        String status = null;
        Map<String,Object> statusBody = null;
        do {
            ResponseEntity<Map> res = rest.getForEntity(statusUrl, Map.class);
            assertThat(res.getStatusCode().is2xxSuccessful()).isTrue();
            statusBody = res.getBody();
            status = String.valueOf(statusBody.get("status"));
            if ("failed".equalsIgnoreCase(status)) {
                throw new AssertionError("Ingest failed: " + statusBody);
            }
            if ("succeeded".equalsIgnoreCase(status)) break;
            Thread.sleep(500);
        } while (Duration.between(start, Instant.now()).getSeconds() < 60);
        assertThat(status).as("ingest status should be succeeded").isEqualTo("succeeded");

        // Ensure counts indicate at least one game and >0 moves/positions
        Object counts = statusBody.get("counts");
        assertThat(counts).as("counts map present").isInstanceOf(Map.class);
        Object gamesCount = ((Map<?,?>) counts).get("games");
        assertThat(Integer.parseInt(String.valueOf(gamesCount)))
                .as("at least one game ingested").isGreaterThanOrEqualTo(1);
        Object movesCount = ((Map<?,?>) counts).get("moves");
        Object positionsCount = ((Map<?,?>) counts).get("positions");
        assertThat(Long.parseLong(String.valueOf(movesCount)))
                .as("at least one move ingested").isGreaterThan(0);
        assertThat(Long.parseLong(String.valueOf(positionsCount)))
                .as("at least one position ingested").isGreaterThan(0);

        // 3) GET /v1/games (list)
        String listUrl = baseUrl() + "/v1/games?username=M3NG00S3&limit=5";
        ResponseEntity<List<Map<String,Object>>> listResp = rest.exchange(listUrl, HttpMethod.GET, null,
                new ParameterizedTypeReference<>() {});
        assertThat(listResp.getStatusCode().is2xxSuccessful()).isTrue();
        List<Map<String,Object>> games = listResp.getBody();
        assertThat(games).isNotNull();
        assertThat(games.size()).isGreaterThanOrEqualTo(1);
        Map<String,Object> first = games.get(0);
        // fields present
        assertThat(first).containsKeys("id","endTime","timeControl","result","whiteRating","blackRating");
        // must not contain PGN fields in list
        assertThat(first).doesNotContainKeys("pgn","pgnRaw");

        // 4) GET /v1/games/{id} (detail)
        UUID gameId = UUID.fromString(String.valueOf(first.get("id")));
        String detailUrl = baseUrl() + "/v1/games/" + gameId;
        ResponseEntity<Map<String,Object>> detailResp = rest.exchange(detailUrl, HttpMethod.GET, null,
                new ParameterizedTypeReference<>() {});
        assertThat(detailResp.getStatusCode().is2xxSuccessful()).isTrue();
        Map<String,Object> detail = detailResp.getBody();
        assertThat(detail).isNotNull();
        assertThat(detail.get("pgnRaw")).as("detail must carry raw PGN").isInstanceOf(String.class);
        assertThat(((String) detail.get("pgnRaw")).length()).isGreaterThan(10);

        // 5) /actuator/prometheus contains chs_ingest_jobs_total with username label
        String promUrl = baseUrl() + "/actuator/prometheus";
        ResponseEntity<String> promResp = rest.exchange(promUrl, HttpMethod.GET, null, String.class);
        if (promResp.getStatusCode().is2xxSuccessful()) {
            String prom = promResp.getBody();
            assertThat(prom).as("prometheus endpoint contains jobs metric").contains("chs_ingest_jobs_total");
            assertThat(prom).as("username label present").contains("username=\"M3NG00S3\"");
        } else {
            // Fallback: JSON metrics endpoint check (environmental differences)
            String jsonMetricUrl = baseUrl() + "/actuator/metrics/chs_ingest_jobs_total";
            ResponseEntity<Map<String,Object>> metricResp = rest.exchange(jsonMetricUrl, HttpMethod.GET, null,
                    new ParameterizedTypeReference<>() {});
            assertThat(metricResp.getStatusCode().is2xxSuccessful())
                    .as("/actuator/metrics responds OK").isTrue();
            Map<String,Object> metric = metricResp.getBody();
            assertThat(metric).isNotNull();
            assertThat(metric.get("name")).isEqualTo("chs_ingest_jobs_total");
        }
    }
}
