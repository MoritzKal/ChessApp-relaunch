package com.chessapp.api.ingest;

import com.chessapp.api.ApiApplication;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.TestConfiguration;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
@SpringBootTest(classes = ApiApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class IngestControllerIT {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine");

    @DynamicPropertySource
    static void databaseProps(DynamicPropertyRegistry registry) {
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

    private String url(String path) {
        return "http://localhost:" + port + path;
    }

    @Test
    void startIngest_returnsRunId() {
        UUID runId = startRun();
        assertThat(runId).isNotNull();
    }

    @Test
    void pollRun_succeedsWithin2s() throws Exception {
        UUID runId = startRun();

        Instant start = Instant.now();
        Map status;
        do {
            ResponseEntity<Map> res = rest.getForEntity(url("/v1/ingest/" + runId), Map.class);
            assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);
            status = res.getBody();
            String s = String.valueOf(status.get("status"));
            if ("SUCCEEDED".equals(s)) {
                break;
            }
            assertThat(Duration.between(start, Instant.now()).toMillis()).isLessThan(2000);
            Thread.sleep(100);
        } while (true);

        assertThat(status.get("reportUri")).isEqualTo("s3://reports/ingest/" + runId + "/report.json");
    }

    @Test
    void dataImportAliasRedirects() {
        ResponseEntity<Void> res = rest.postForEntity(url("/v1/data/import"), null, Void.class);
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.PERMANENT_REDIRECT);
        assertThat(res.getHeaders().getLocation()).hasToString("/v1/ingest");
    }

    @Test
    void prometheusExposesMetrics() {
        startRun();
        String body = rest.getForObject(url("/actuator/prometheus"), String.class);
        Pattern pattern = Pattern.compile("^chs_ingest_runs_started_total\\{.*}\\s+(\\d+(?:\\.\\d+)?)$", Pattern.MULTILINE);
        Matcher m = pattern.matcher(body);
        assertThat(m.find()).isTrue();
        double val = Double.parseDouble(m.group(1));
        assertThat(val).isGreaterThan(0.0);
    }

    private UUID startRun() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        Map<String, Object> req = Map.of("username", "alice");
        ResponseEntity<Map> resp = rest.postForEntity(url("/v1/ingest"), new HttpEntity<>(req, headers), Map.class);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);
        String runId = (String) resp.getBody().get("runId");
        return UUID.fromString(runId);
    }

    @TestConfiguration
    static class NoSleepConfig {
        @Bean
        Supplier<Long> ingestSleep() {
            return () -> 0L;
        }
    }
}

