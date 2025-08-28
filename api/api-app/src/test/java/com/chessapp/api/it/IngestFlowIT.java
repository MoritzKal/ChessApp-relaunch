package com.chessapp.api.it;

import com.chessapp.api.codex.CodexApplication;
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
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
@SpringBootTest(classes = CodexApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("codex")
class IngestFlowIT {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine");

    @DynamicPropertySource
    static void registerProps(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
        registry.add("spring.flyway.enabled", () -> true);
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "validate");
    }

    @LocalServerPort
    int port;

    @Autowired
    TestRestTemplate rest;

    private String baseUrl() {
        return "http://localhost:" + port;
    }

    @Test
    @Timeout(90)
    void endToEnd_offlineIngest_thenListAndDetail_andPrometheusHasMetrics() throws Exception {
        // 1) Start ingest (offline=true)
        String startUrl = baseUrl() + "/v1/ingest";
        Map<String,Object> req = new HashMap<>();
        req.put("username", "M3NG00S3");
        req.put("offline", true);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        ResponseEntity<Map> startResp = rest.postForEntity(new URI(startUrl), new HttpEntity<>(req, headers), Map.class);
        assertThat(startResp.getStatusCode().is2xxSuccessful() || startResp.getStatusCode().is3xxRedirection()).isTrue();
        Object runIdObj = startResp.getBody().get("runId");
        assertThat(runIdObj).isInstanceOf(String.class);
        UUID runId = UUID.fromString((String) runIdObj);

        // 2) Poll status until succeeded (or timeout)
        String statusUrl = baseUrl() + "/v1/ingest/" + runId;
        Instant start = Instant.now();
        String status;
        Map<String,Object> statusBody;
        do {
            ResponseEntity<Map> res = rest.getForEntity(statusUrl, Map.class);
            assertThat(res.getStatusCode().is2xxSuccessful()).isTrue();
            statusBody = res.getBody();
            status = String.valueOf(statusBody.get("status"));
            if ("failed".equalsIgnoreCase(status)) {
                throw new AssertionError("Ingest failed: " + statusBody);
            }
            if ("succeeded".equalsIgnoreCase(status)) break;
            Thread.sleep(300);
        } while (Duration.between(start, Instant.now()).getSeconds() < 60);
        assertThat(status).isEqualTo("succeeded");

        // Ensure counts indicate at least one game
        Object counts = statusBody.get("counts");
        assertThat(counts).isInstanceOf(Map.class);
        Object gamesCount = ((Map<?,?>) counts).get("games");
        assertThat(Integer.parseInt(String.valueOf(gamesCount))).isGreaterThanOrEqualTo(1);

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
        // must not contain pgn fields in list
        assertThat(first).doesNotContainKeys("pgn","pgnRaw");

        // 4) GET /v1/games/{id} (detail)
        UUID gameId = UUID.fromString(String.valueOf(first.get("id")));
        String detailUrl = baseUrl() + "/v1/games/" + gameId;
        ResponseEntity<Map<String,Object>> detailResp = rest.exchange(detailUrl, HttpMethod.GET, null,
                new ParameterizedTypeReference<>() {});
        assertThat(detailResp.getStatusCode().is2xxSuccessful()).isTrue();
        Map<String,Object> detail = detailResp.getBody();
        assertThat(detail).isNotNull();
        assertThat(detail.get("pgnRaw")).isInstanceOf(String.class);
        assertThat(((String) detail.get("pgnRaw")).length()).isGreaterThan(10);

        // 5) /actuator/prometheus contains chs_ingest_jobs_total with username label
        String promUrl = baseUrl() + "/actuator/prometheus";
        String prom = rest.getForObject(promUrl, String.class);
        assertThat(prom).contains("chs_ingest_jobs_total");
        Pattern line = Pattern.compile("^chs_ingest_jobs_total\\{.*username=\"M3NG00S3\".*}\\s+(\\d+(?:\\.\\d+)?)$", Pattern.MULTILINE);
        Matcher m = line.matcher(prom);
        assertThat(m.find()).as("metric line with username label present").isTrue();
        double val = Double.parseDouble(m.group(1));
        assertThat(val).isGreaterThanOrEqualTo(1.0);
    }
}

