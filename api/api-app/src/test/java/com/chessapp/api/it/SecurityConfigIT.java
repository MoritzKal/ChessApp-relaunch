package com.chessapp.api.it;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Duration;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import com.chessapp.api.testutil.AbstractIntegrationTest;
import com.chessapp.api.support.JwtTestUtils;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    classes = com.chessapp.api.codex.CodexApplication.class)
@ActiveProfiles("codex")
@AutoConfigureMockMvc
class SecurityConfigIT extends AbstractIntegrationTest {

    @Autowired
    MockMvc mockMvc;

    @Value("${app.security.jwt.secret}")
    String secret;

    @Test
    void v1_withoutToken_returns401() throws Exception {
        mockMvc.perform(get("/v1/datasets"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void v1_withUserToken_returns200() throws Exception {
        String token = JwtTestUtils.signHmac256(secret, Map.of(
                "preferred_username", "user1",
                "roles", List.of("USER"),
                "scope", "api.read"), Duration.ofMinutes(10));
        mockMvc.perform(get("/v1/datasets")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk());
    }

    @Test
    void apiDocs_and_swagger_are_public() throws Exception {
        mockMvc.perform(get("/v3/api-docs.yaml"))
                .andExpect(status().isOk());
        mockMvc.perform(get("/swagger-ui/index.html"))
                .andExpect(status().isOk());
    }

    @Test
    void prometheus_requires_monitoring_role() throws Exception {
        String userToken = JwtTestUtils.signHmac256(secret, Map.of(
                "sub", "user1",
                "roles", List.of("USER")), Duration.ofMinutes(5));
        mockMvc.perform(get("/actuator/prometheus")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + userToken))
                .andExpect(status().isForbidden());

        String monitoringToken = JwtTestUtils.signHmac256(secret, Map.of(
                "sub", "mon",
                "roles", List.of("MONITORING")), Duration.ofMinutes(5));
        mockMvc.perform(get("/actuator/prometheus")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + monitoringToken))
                .andExpect(status().isOk());
    }

    @Test
    void cors_preflight_ok() throws Exception {
        mockMvc.perform(options("/v1/datasets")
                        .header("Origin", "http://localhost:5173")
                        .header("Access-Control-Request-Method", "GET")
                        .header("Access-Control-Request-Headers", "Authorization,Content-Type"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:5173"))
                .andExpect(header().string("Vary", containsString("Origin")));
    }
}
