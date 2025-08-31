package com.chessapp.api.dataset;

import com.chessapp.api.codex.CodexApplication;
import com.chessapp.api.service.dto.DatasetCreateRequest;
import com.chessapp.api.service.dto.DatasetResponse;
import com.chessapp.api.testutil.AbstractIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

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

    @MockBean
    S3Client s3Client;

    @BeforeEach
    void setup() {
        when(s3Client.putObject(ArgumentMatchers.any(PutObjectRequest.class), ArgumentMatchers.any(RequestBody.class)))
                .thenReturn(PutObjectResponse.builder().build());
    }

    @Test
    void create_and_get_and_list() {
        String baseUrl = "http://localhost:" + port + "/v1/datasets";
        DatasetCreateRequest req = new DatasetCreateRequest();
        req.setName("train");
        req.setVersion("1.0.0");
        req.setFilter(Map.of("foo", "bar"));
        req.setSplit(Map.of("train", 0.8, "test", 0.2));
        req.setSizeRows(10L);

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

        ResponseEntity<DatasetResponse[]> listResp = rest.getForEntity(baseUrl, DatasetResponse[].class);
        assertThat(listResp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(listResp.getBody()).isNotEmpty();
    }

    @Test
    void validation_errors_return_bad_request() {
        String baseUrl = "http://localhost:" + port + "/v1/datasets";
        DatasetCreateRequest req = new DatasetCreateRequest();
        // missing required fields
        ResponseEntity<String> resp = rest.postForEntity(baseUrl, req, String.class);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }
}
