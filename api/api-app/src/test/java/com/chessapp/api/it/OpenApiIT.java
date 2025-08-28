package com.chessapp.api.it;

import com.chessapp.api.codex.CodexApplication;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(
        classes = CodexApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                // Avoid minimal logback-codex.xml provider error in tests
                "logging.config=classpath:logback-spring.xml"
        }
)
@ActiveProfiles("codex")
class OpenApiIT extends com.chessapp.api.testutil.AbstractIntegrationTest {

    @LocalServerPort
    int port;

    @Autowired
    TestRestTemplate rest;

    @Test
    void openapi_isAvailable_when_app_is_running_in_test() throws Exception {
        String url = "http://localhost:" + port + "/v3/api-docs";
        ResponseEntity<String> resp = rest.getForEntity(url, String.class);
        assertThat(resp.getStatusCode().is2xxSuccessful()).isTrue();
        String body = resp.getBody();
        assertThat(body).isNotBlank();

        ObjectMapper mapper = new ObjectMapper();
        JsonNode root = mapper.readTree(body);
        assertThat(root.has("openapi")).isTrue();
        assertThat(root.path("paths").isObject()).isTrue();
    }
}

