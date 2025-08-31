package com.chessapp.api.security;

import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import com.chessapp.api.testutil.AbstractIntegrationTest;

@SpringBootTest(classes = com.chessapp.api.codex.CodexApplication.class)
@AutoConfigureMockMvc
@TestPropertySource(properties = "security.scrape.token=demo-scrape-token")
class SecurityIT extends AbstractIntegrationTest {

    @Autowired
    MockMvc mvc;
    @Autowired
    ObjectMapper mapper;

    @Test
    void datasets_require_jwt() throws Exception {
        mvc.perform(get("/v1/datasets"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void health_requires_admin_role() throws Exception {
        String userToken = login("user", "pw");
        mvc.perform(get("/actuator/health").header("Authorization", "Bearer " + userToken))
                .andExpect(status().isForbidden());

        String adminToken = login("admin", "password");
        mvc.perform(get("/actuator/health").header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());
    }

    @Test
    void prometheus_allows_scrape_token() throws Exception {
        mvc.perform(get("/actuator/prometheus")
                        .header("Authorization", "Bearer demo-scrape-token"))
                .andExpect(status().isOk());
    }

    @Test
    void cors_preflight_succeeds() throws Exception {
        mvc.perform(options("/v1/datasets")
                        .header("Origin", "http://localhost:3000")
                        .header("Access-Control-Request-Method", "GET"))
                .andExpect(status().isOk());
    }

    @Test
    void login_endpoint_returns_token() throws Exception {
        mvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(Map.of("username", "user", "password", "pw"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token", notNullValue()));
    }

    private String login(String username, String password) throws Exception {
        MvcResult result = mvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(Map.of("username", username, "password", password))))
                .andExpect(status().isOk())
                .andReturn();
        return mapper.readTree(result.getResponse().getContentAsString()).get("token").asText();
    }
}
