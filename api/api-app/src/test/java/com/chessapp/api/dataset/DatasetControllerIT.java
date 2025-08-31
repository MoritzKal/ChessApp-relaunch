package com.chessapp.api.dataset;

import com.chessapp.api.codex.CodexApplication;
import com.chessapp.api.dataset.dto.CreateDatasetRequest;
import com.chessapp.api.dataset.dto.DatasetResponse;
import com.chessapp.api.testutil.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(
        classes = CodexApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "logging.config=classpath:logback-spring.xml"
        }
)
@ActiveProfiles("codex")
class DatasetControllerIT extends AbstractIntegrationTest {

    @LocalServerPort
    int port;

    @Autowired
    TestRestTemplate rest;

    @Test
    void create_and_get_and_list() {
        String baseUrl = "http://localhost:" + port + "/v1/datasets";
        CreateDatasetRequest req = new CreateDatasetRequest();
        req.setName("train");
        req.setVersion("1.0.0");
        req.setFilterJson("{\"foo\":\"bar\"}");
        req.setSplitJson("{\"train\":0.8,\"test\":0.2}");
        req.setSizeRows(10L);
        req.setLocationUri("s3://datasets/train");

        ResponseEntity<DatasetResponse> createResp = rest.postForEntity(baseUrl, req, DatasetResponse.class);
        assertThat(createResp.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(createResp.getHeaders().getLocation()).isNotNull();
        DatasetResponse created = createResp.getBody();
        assertThat(created).isNotNull();
        UUID id = created.getId();

        ResponseEntity<DatasetResponse> getResp = rest.getForEntity(baseUrl + "/" + id, DatasetResponse.class);
        assertThat(getResp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(getResp.getBody()).isNotNull();
        assertThat(getResp.getBody().getId()).isEqualTo(id);

        ResponseEntity<JsonNode> listResp = rest.getForEntity(baseUrl, JsonNode.class);
        assertThat(listResp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(listResp.getBody().path("content")).isNotEmpty();
    }

    @Test
    void validation_errors_return_bad_request() {
        String baseUrl = "http://localhost:" + port + "/v1/datasets";
        CreateDatasetRequest req = new CreateDatasetRequest();
        // missing required fields
        ResponseEntity<String> resp = rest.postForEntity(baseUrl, req, String.class);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }
}
