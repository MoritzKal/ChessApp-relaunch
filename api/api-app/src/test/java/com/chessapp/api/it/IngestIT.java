package com.chessapp.api.it;

import com.chessapp.api.codex.CodexApplication;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
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
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
@SpringBootTest(
        classes = CodexApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "logging.config=classpath:logback-spring.xml",
                "management.prometheus.metrics.export.enabled=true"
        }
)
@ActiveProfiles("codex")
class IngestIT {

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
    @Timeout(30)
    void ingestFlow_andMetrics() throws Exception {
        // start ingest
        String startUrl = baseUrl() + "/v1/ingest";
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        ResponseEntity<Map> startResp = rest.postForEntity(new URI(startUrl),
                new HttpEntity<>(Map.of("username", "M3NG00S3"), headers), Map.class);
        assertThat(startResp.getStatusCode().value()).isEqualTo(202);
        UUID runId = UUID.fromString(startResp.getBody().get("runId").toString());

        // alias
        ResponseEntity<Void> aliasResp = rest.postForEntity(new URI(baseUrl()+"/v1/data/import"), null, Void.class);
        assertThat(aliasResp.getStatusCode().value()).isEqualTo(308);
        assertThat(aliasResp.getHeaders().getLocation().getPath()).isEqualTo("/v1/ingest");

        // poll status
        String statusUrl = baseUrl() + "/v1/ingest/" + runId;
        Instant start = Instant.now();
        String status;
        String reportUri = null;
        do {
            ResponseEntity<Map> res = rest.getForEntity(statusUrl, Map.class);
            assertThat(res.getStatusCode().is2xxSuccessful()).isTrue();
            status = String.valueOf(res.getBody().get("status"));
            reportUri = (String) res.getBody().get("reportUri");
            if ("SUCCEEDED".equals(status) || "FAILED".equals(status)) break;
            Thread.sleep(200);
        } while (Duration.between(start, Instant.now()).getSeconds() < 10);
        assertThat(status).isEqualTo("SUCCEEDED");
        assertThat(reportUri).isEqualTo("s3://reports/ingest/" + runId + "/report.json");

        // metrics
        ResponseEntity<Map> metric = rest.getForEntity(baseUrl()+"/actuator/metrics/chs_ingest_jobs_total", Map.class);
        assertThat(metric.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(metric.getBody()).isNotNull();
        assertThat(metric.getBody().toString()).contains("chs_ingest_jobs_total");
    }
}
