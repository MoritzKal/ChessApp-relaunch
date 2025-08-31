package com.chessapp.api.ingest;

import com.chessapp.api.codex.CodexApplication;
import com.chessapp.api.support.JwtTestUtils;
import com.chessapp.api.testutil.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.*;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(
        classes = CodexApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "logging.config=classpath:logback-spring.xml"
        }
)
class IngestApiIT extends AbstractIntegrationTest {

    @LocalServerPort
    int port;

    @Autowired
    TestRestTemplate rest;

    @Test
    void happyPath() {
        String baseUrl = "http://localhost:" + port + "/v1/ingest";
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(JwtTestUtils.createToken("it-user", JwtTestUtils.SECRET, "ROLE_USER"));
        HttpEntity<String> entity = new HttpEntity<>("{}", headers);
        ResponseEntity<Map> createResp = rest.postForEntity(baseUrl, entity, Map.class);
        assertThat(createResp.getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);
        String runId = (String) createResp.getBody().get("runId");

        HttpHeaders getHeaders = new HttpHeaders();
        getHeaders.setBearerAuth(JwtTestUtils.createToken("it-user", JwtTestUtils.SECRET, "ROLE_USER"));
        ResponseEntity<String> statusResp = rest.exchange(
                baseUrl + "/" + runId,
                HttpMethod.GET,
                new HttpEntity<Void>(getHeaders),
                String.class);
        assertThat(statusResp.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void unauthorizedWithoutToken() {
        String baseUrl = "http://localhost:" + port + "/v1/ingest";
        ResponseEntity<String> resp = rest.postForEntity(baseUrl, new HttpEntity<>("{}"), String.class);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void actuatorPrometheusSecured() {
        String url = "http://localhost:" + port + "/actuator/prometheus";
        ResponseEntity<String> noAuth = rest.getForEntity(url, String.class);
        assertThat(noAuth.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(JwtTestUtils.createToken("monitor", JwtTestUtils.SECRET, "ROLE_MONITORING"));
        ResponseEntity<String> ok = rest.exchange(
                url,
                HttpMethod.GET,
                new HttpEntity<Void>(headers),
                String.class);
        assertThat(ok.getStatusCode()).isEqualTo(HttpStatus.OK);
    }
}
