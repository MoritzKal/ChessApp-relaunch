package com.chessapp.api.dataset;

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
                "logging.config=classpath:logback-spring.xml"
        }
)
@ActiveProfiles("codex")
class DatasetOpenApiIT extends com.chessapp.api.testutil.AbstractIntegrationTest {

    @LocalServerPort
    int port;

    @Autowired
    TestRestTemplate rest;

    @Test
    void openapi_contains_dataset_paths() throws Exception {
        String url = "http://localhost:" + port + "/v3/api-docs";
        ResponseEntity<String> resp = rest.getForEntity(url, String.class);
        assertThat(resp.getStatusCode().is2xxSuccessful()).isTrue();
        ObjectMapper mapper = new ObjectMapper();
        JsonNode root = mapper.readTree(resp.getBody());
        JsonNode paths = root.path("paths");
        assertThat(paths.has("/v1/datasets")).isTrue();
        assertThat(paths.path("/v1/datasets").has("post")).isTrue();
        assertThat(paths.path("/v1/datasets").has("get")).isTrue();
        assertThat(paths.has("/v1/datasets/{id}")).isTrue();
        assertThat(paths.path("/v1/datasets/{id}").has("get")).isTrue();
    }

    @Test
    void swagger_ui_available() {
        String url = "http://localhost:" + port + "/swagger-ui.html";
        ResponseEntity<String> resp = rest.getForEntity(url, String.class);
        assertThat(resp.getStatusCode().is2xxSuccessful()).isTrue();
    }
}
