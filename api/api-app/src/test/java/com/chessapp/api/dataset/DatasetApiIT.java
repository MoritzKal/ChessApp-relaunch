package com.chessapp.api.dataset;

import com.chessapp.api.codex.CodexApplication;
import com.chessapp.api.support.JwtTestUtils;
import com.chessapp.api.testutil.AbstractIntegrationTest;
import com.jayway.jsonpath.JsonPath;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import software.amazon.awssdk.services.s3.S3Client;

import static org.mockito.Mockito.mock;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(
    classes = CodexApplication.class,
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = {"logging.config=classpath:logback-spring.xml"}
)
@AutoConfigureMockMvc
class DatasetApiIT extends AbstractIntegrationTest {

    @Autowired
    MockMvc mockMvc;

    @TestConfiguration
    static class TestCfg {
        @Bean
        S3Client s3Client() { return mock(S3Client.class); }
    }

    @Test
    void dataset_crud_flow() throws Exception {
        String body = "{\"name\":\"d1\",\"version\":\"v1\",\"sizeRows\":1}";
        var token = JwtTestUtils.token("alice", List.of("USER"));
        var post = mockMvc.perform(post("/v1/datasets")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(body))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", org.hamcrest.Matchers.matchesPattern("/v1/datasets/.+")))
                .andReturn();
        UUID id = UUID.fromString(JsonPath.read(post.getResponse().getContentAsString(), "$.id"));

        mockMvc.perform(get("/v1/datasets/{id}", id)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id.toString()));

        mockMvc.perform(get("/v1/datasets?limit=10&offset=0")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(id.toString()));
    }

    @Test
    void dataset_requires_auth() throws Exception {
        mockMvc.perform(get("/v1/datasets"))
                .andExpect(status().isUnauthorized());

        String token = JwtTestUtils.token("bob", List.of("MONITORING"));
        mockMvc.perform(get("/v1/datasets").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isForbidden());
    }

    @Test
    void cors_preflight() throws Exception {
        mockMvc.perform(options("/v1/datasets")
                        .header(HttpHeaders.ORIGIN, "http://localhost:3000")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "http://localhost:3000"))
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_METHODS, org.hamcrest.Matchers.containsString("POST")))
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_HEADERS, org.hamcrest.Matchers.containsString("Authorization")));
    }
}
