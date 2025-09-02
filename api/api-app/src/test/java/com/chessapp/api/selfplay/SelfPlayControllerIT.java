package com.chessapp.api.selfplay;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.UUID;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;

@SpringBootTest
@AutoConfigureMockMvc
class SelfPlayControllerIT {

    private static final WireMockServer wm = new WireMockServer(WireMockConfiguration.options().dynamicPort());

    @DynamicPropertySource
    static void configure(DynamicPropertyRegistry registry) {
        wm.start();
        registry.add("clients.selfplayRunner.baseUrl", wm::baseUrl);
    }

    @AfterAll
    static void shutdown() {
        wm.stop();
    }

    @Autowired
    MockMvc mvc;

    @BeforeEach
    void resetWiremock() {
        wm.resetAll();
    }

    @Test
    void postRunHappyPath() throws Exception {
        wm.stubFor(WireMock.post("/runner/selfplay/start")
                .withHeader("Idempotency-Key", equalTo("abc"))
                .willReturn(aResponse().withStatus(201)
                        .withBody("{\"runId\":\"" + UUID.randomUUID() + "\"}")));

        mvc.perform(post("/v1/selfplay/runs")
                        .with(jwt().jwt(j -> j.claim("scope", "selfplay").claim("preferred_username", "bob")))
                        .header("Idempotency-Key", "abc")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"modelId\":\"m1\",\"baselineId\":\"b1\",\"games\":1,\"concurrency\":1,\"seed\":1}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.runId").exists());

        wm.verify(postRequestedFor(urlEqualTo("/runner/selfplay/start"))
                .withHeader("Idempotency-Key", equalTo("abc")));
    }

    @Test
    void getRunHappyPath() throws Exception {
        wm.stubFor(WireMock.get(urlEqualTo("/runner/selfplay/runs/123"))
                .willReturn(aResponse().withStatus(200)
                        .withBody("{\"runId\":\"123\",\"status\":\"running\"}")));

        mvc.perform(get("/v1/selfplay/runs/123")
                        .with(jwt().jwt(j -> j.claim("scope", "selfplay").claim("preferred_username", "bob"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("running"));
    }

    @Test
    void noJwtUnauthorized() throws Exception {
        mvc.perform(get("/v1/selfplay/runs/123"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void wrongScopeForbidden() throws Exception {
        mvc.perform(get("/v1/selfplay/runs/123")
                        .with(jwt().jwt(j -> j.claim("scope", "other"))))
                .andExpect(status().isForbidden());
    }

    @Test
    void rateLimit() throws Exception {
        wm.stubFor(WireMock.get(urlMatching("/runner/selfplay/runs/.*"))
                .willReturn(aResponse().withStatus(200).withBody("{}")));
        for (int i = 0; i < 60; i++) {
            mvc.perform(get("/v1/selfplay/runs/" + i)
                    .with(jwt().jwt(j -> j.claim("scope", "selfplay").claim("preferred_username", "alice"))))
                    .andExpect(status().isOk());
        }
        mvc.perform(get("/v1/selfplay/runs/overflow")
                .with(jwt().jwt(j -> j.claim("scope", "selfplay").claim("preferred_username", "alice"))))
                .andExpect(status().isTooManyRequests());
    }

    @Test
    void upstream500Returns502() throws Exception {
        wm.stubFor(WireMock.get(urlEqualTo("/runner/selfplay/runs/err"))
                .willReturn(aResponse().withStatus(500)));
        mvc.perform(get("/v1/selfplay/runs/err")
                .with(jwt().jwt(j -> j.claim("scope", "selfplay").claim("preferred_username", "bob"))))
                .andExpect(status().isBadGateway());
    }
}
