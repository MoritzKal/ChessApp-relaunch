package com.chessapp.api.eval;

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
class EvalControllerIT {

    private static final WireMockServer wm = new WireMockServer(WireMockConfiguration.options().dynamicPort());

    @DynamicPropertySource
    static void configure(DynamicPropertyRegistry registry) {
        wm.start();
        registry.add("evalOffline.baseUrl", wm::baseUrl);
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
    void postEvalHappyPath() throws Exception {
        wm.stubFor(WireMock.post("/runner/eval/start")
                .withHeader("Idempotency-Key", equalTo("abc"))
                .willReturn(aResponse().withStatus(202)
                        .withBody("{\"evalId\":\"" + UUID.randomUUID() + "\"}")));

        mvc.perform(post("/v1/evaluations")
                        .with(jwt().jwt(j -> j.claim("scope", "eval").claim("preferred_username", "bob")))
                        .header("Idempotency-Key", "abc")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"modelId\":\"m1\",\"datasetId\":\"d1\",\"metrics\":[\"accuracy\"]}"))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.evalId").exists());

        wm.verify(postRequestedFor(urlEqualTo("/runner/eval/start"))
                .withHeader("Idempotency-Key", equalTo("abc")));
    }

    @Test
    void getEvalHappyPath() throws Exception {
        wm.stubFor(WireMock.get(urlEqualTo("/runner/eval/123"))
                .willReturn(aResponse().withStatus(200)
                        .withBody("{\"evalId\":\"123\",\"status\":\"running\"}")));

        mvc.perform(get("/v1/evaluations/123")
                        .with(jwt().jwt(j -> j.claim("scope", "eval").claim("preferred_username", "bob"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("running"));
    }

    @Test
    void noJwtUnauthorized() throws Exception {
        mvc.perform(get("/v1/evaluations/123"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void wrongScopeForbidden() throws Exception {
        mvc.perform(get("/v1/evaluations/123")
                        .with(jwt().jwt(j -> j.claim("scope", "other"))))
                .andExpect(status().isForbidden());
    }

    @Test
    void rateLimit() throws Exception {
        wm.stubFor(WireMock.get(urlMatching("/runner/eval/.*"))
                .willReturn(aResponse().withStatus(200).withBody("{}")));
        for (int i = 0; i < 60; i++) {
            mvc.perform(get("/v1/evaluations/" + i)
                    .with(jwt().jwt(j -> j.claim("scope", "eval").claim("preferred_username", "alice"))))
                    .andExpect(status().isOk());
        }
        mvc.perform(get("/v1/evaluations/overflow")
                .with(jwt().jwt(j -> j.claim("scope", "eval").claim("preferred_username", "alice"))))
                .andExpect(status().isTooManyRequests());
    }

    @Test
    void upstream500Returns503() throws Exception {
        wm.stubFor(WireMock.get(urlEqualTo("/runner/eval/err"))
                .willReturn(aResponse().withStatus(500)));
        mvc.perform(get("/v1/evaluations/err")
                .with(jwt().jwt(j -> j.claim("scope", "eval").claim("preferred_username", "bob"))))
                .andExpect(status().isServiceUnavailable());
    }
}
