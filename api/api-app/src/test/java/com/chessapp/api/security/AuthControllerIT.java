package com.chessapp.api.security;

import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.context.ActiveProfiles;

import com.chessapp.api.testutil.AbstractIntegrationTest;

@SpringBootTest(classes = com.chessapp.api.codex.CodexApplication.class)
@AutoConfigureMockMvc
@ActiveProfiles("codex")
class AuthControllerIT extends AbstractIntegrationTest {

    @Autowired
    MockMvc mvc;
    @Autowired
    ObjectMapper mapper;

    @Test
    void login_and_access_protected_endpoint() throws Exception {
        MvcResult result = mvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(Map.of("username", "user", "password", "pw"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token", notNullValue()))
                .andReturn();
        String token = mapper.readTree(result.getResponse().getContentAsString()).get("token").asText();

        mvc.perform(get("/v1/models")).andExpect(status().isUnauthorized());

        mvc.perform(get("/v1/models").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }

    @Test
    void prometheus_requires_token() throws Exception {
        mvc.perform(get("/actuator/prometheus"))
                .andExpect(status().isUnauthorized());
        mvc.perform(get("/actuator/prometheus").header("Authorization", "Bearer change_me"))
                .andExpect(status().isOk());
    }
}
