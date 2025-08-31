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
import com.chessapp.api.support.JwtTestUtils;
import com.chessapp.api.testutil.AbstractIntegrationTest;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(
        classes = CodexApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "logging.config=classpath:logback-spring.xml"
        }
)
class DatasetApiIT extends AbstractIntegrationTest {

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
        headers.setBearerAuth(JwtTestUtils.createToken("it-user", JwtTestUtils.SECRET, "ROLE_USER"));
        HttpEntity<String> entity = new HttpEntity<>(json, headers);

        ResponseEntity<DatasetResponse> createResp = rest.postForEntity(baseUrl, entity, DatasetResponse.class);
        assertThat(createResp.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(createResp.getHeaders().getLocation()).isNotNull();
        String location = createResp.getHeaders().getLocation().toString();

        HttpHeaders getHeaders = new HttpHeaders();
        getHeaders.setBearerAuth(JwtTestUtils.createToken("it-user", JwtTestUtils.SECRET, "ROLE_USER"));
        ResponseEntity<DatasetResponse> getResp = rest.exchange(
                "http://localhost:" + port + location,
                org.springframework.http.HttpMethod.GET,
                new HttpEntity<Void>(getHeaders),
                DatasetResponse.class);
        assertThat(getResp.getStatusCode()).isEqualTo(HttpStatus.OK);
        DatasetResponse body = getResp.getBody();
        assertThat(body).isNotNull();
        assertThat(body.getId()).isNotNull();
        assertThat(body.getName()).isEqualTo("user-games");
        assertThat(body.getVersion()).isEqualTo("1.0.0");
        assertThat(body.getLocationUri()).isEqualTo("s3://minio/datasets/user-games-1.0.0");

        HttpHeaders listHeaders = new HttpHeaders();
        listHeaders.setBearerAuth(JwtTestUtils.createToken("it-user", JwtTestUtils.SECRET, "ROLE_USER"));
        ResponseEntity<String> listResp = rest.exchange(
                baseUrl + "?page=0&size=10",
                org.springframework.http.HttpMethod.GET,
                new HttpEntity<Void>(listHeaders),
                String.class);
        assertThat(listResp.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void unauthorizedWithoutToken() {
        String baseUrl = "http://localhost:" + port + "/v1/datasets";
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> entity = new HttpEntity<>("{}", headers);
        ResponseEntity<String> resp = rest.postForEntity(baseUrl, entity, String.class);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void corsPreflight() {
        String baseUrl = "http://localhost:" + port + "/v1/datasets";
        HttpHeaders headers = new HttpHeaders();
        headers.setOrigin("http://localhost:5173");
        headers.setAccessControlRequestMethod(org.springframework.http.HttpMethod.POST);
        headers.add("Access-Control-Request-Headers", "Authorization,Content-Type");
        ResponseEntity<String> resp = rest.exchange(
                baseUrl,
                org.springframework.http.HttpMethod.OPTIONS,
                new HttpEntity<Void>(headers),
                String.class);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp.getHeaders().getAccessControlAllowOrigin()).isEqualTo("http://localhost:5173");
    }
}

