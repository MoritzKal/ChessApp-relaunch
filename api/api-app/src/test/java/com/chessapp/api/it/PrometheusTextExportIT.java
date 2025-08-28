package com.chessapp.api.it;

import com.chessapp.api.codex.CodexApplication;
import com.chessapp.api.testutil.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;

import java.util.regex.Pattern;

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
class PrometheusTextExportIT extends AbstractIntegrationTest {

    @LocalServerPort
    int port;

    @Autowired
    TestRestTemplate rest;

    @Test
    void prometheusEndpointExportsMetrics() {
        String url = "http://localhost:" + port + "/actuator/prometheus";
        ResponseEntity<String> resp = rest.getForEntity(url, String.class);
        assertThat(resp.getStatusCode().value()).isEqualTo(200);
        assertThat(resp.getHeaders().getFirst("Content-Type"))
                .startsWith("text/plain; version=0.0.4");
        String body = resp.getBody();
        assertThat(body).isNotBlank();
        Pattern pattern = Pattern.compile(
                "^chs_ingest_games_total\\{[^}]*username=\"M3NG00S3\"[^}]*}.+$",
                Pattern.MULTILINE);
        assertThat(pattern.matcher(body).find()).isTrue();
    }
}
