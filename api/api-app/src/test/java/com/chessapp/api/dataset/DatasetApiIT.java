package com.chessapp.api.dataset;

import com.chessapp.api.codex.CodexApplication;
import com.chessapp.api.dataset.dto.DatasetResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(
        classes = CodexApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "logging.config=classpath:logback-spring.xml"
        }
)
@Testcontainers
@ActiveProfiles("codex")
class DatasetApiIT {

    @Container
    static final PostgreSQLContainer<?> postgres =
            new PostgreSQLContainer<>("postgres:16-alpine");

    @DynamicPropertySource
    static void registerDataSourceProps(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
        registry.add("spring.flyway.enabled", () -> true);
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "validate");
        registry.add("spring.jpa.properties.hibernate.prefer_native_enum_types", () -> "true");
        registry.add("spring.jpa.properties.hibernate.type.preferred_enum_jdbc_type", () -> "postgres_enum");
    }

    @LocalServerPort
    int port;

    @Autowired
    TestRestTemplate rest;

    @Test
    void happyPath() {
        String baseUrl = "http://localhost:" + port + "/v1/datasets";

        String json = "{" +
                "\"name\":\"user-games\"," +
                "\"version\":\"1.0.0\"," +
                "\"filterJson\":\"{\\\"since\\\":\\\"2025-01-01\\\"}\"," +
                "\"splitJson\":\"{\\\"train\\\":0.8,\\\"val\\\":0.1,\\\"test\\\":0.1}\"," +
                "\"sizeRows\":12345," +
                "\"locationUri\":\"s3://minio/datasets/user-games-1.0.0\"" +
                "}";
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("X-Debug-User", "it-user");
        HttpEntity<String> entity = new HttpEntity<>(json, headers);

        ResponseEntity<DatasetResponse> createResp = rest.postForEntity(baseUrl, entity, DatasetResponse.class);
        assertThat(createResp.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(createResp.getHeaders().getLocation()).isNotNull();
        String location = createResp.getHeaders().getLocation().toString();

        ResponseEntity<DatasetResponse> getResp = rest.getForEntity("http://localhost:" + port + location, DatasetResponse.class);
        assertThat(getResp.getStatusCode()).isEqualTo(HttpStatus.OK);
        DatasetResponse body = getResp.getBody();
        assertThat(body).isNotNull();
        assertThat(body.getId()).isNotNull();
        assertThat(body.getName()).isEqualTo("user-games");
        assertThat(body.getVersion()).isEqualTo("1.0.0");
        assertThat(body.getLocationUri()).isEqualTo("s3://minio/datasets/user-games-1.0.0");

        ResponseEntity<String> listResp = rest.getForEntity(baseUrl + "?page=0&size=10", String.class);
        assertThat(listResp.getStatusCode()).isEqualTo(HttpStatus.OK);
    }
}

