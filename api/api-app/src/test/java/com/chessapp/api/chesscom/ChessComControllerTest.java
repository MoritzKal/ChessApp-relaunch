package com.chessapp.api.chesscom;

import com.chessapp.api.codex.CodexApplication;
import com.chessapp.api.testutil.TestAuth;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import com.github.tomakehurst.wiremock.client.WireMock;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {"chess.ingest.baseUrl=http://localhost:${wiremock.server.port}"},
        classes = CodexApplication.class)
@AutoConfigureMockMvc
@WireMockTest(httpPort = 0)
class ChessComControllerTest {

    @Autowired MockMvc mvc;

    @Test
    void archives_ok() throws Exception {
        WireMock.stubFor(WireMock.get(WireMock.urlPathEqualTo("/pub/player/testuser/games/archives"))
                .willReturn(WireMock.okJson("{\"archives\":[\"http://x/pub/player/testuser/games/2024/01\"]}")));
        mvc.perform(get("/v1/chesscom/archives").param("user","testuser").with(TestAuth.jwtUser()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.months[0]").value("2024-01"));
    }

    @Test
    void archives_404() throws Exception {
        WireMock.stubFor(WireMock.get(WireMock.urlPathEqualTo("/pub/player/unknown/games/archives"))
                .willReturn(WireMock.aResponse().withStatus(404)));
        mvc.perform(get("/v1/chesscom/archives").param("user","unknown").with(TestAuth.jwtUser()))
                .andExpect(status().isNotFound());
    }

    @Test
    void meta_ok() throws Exception {
        WireMock.stubFor(WireMock.get(WireMock.urlPathEqualTo("/pub/player/testuser/games/2024/01"))
                .willReturn(WireMock.okJson("{\"games\":[{\"time_class\":\"rapid\"},{\"time_class\":\"blitz\"}]}")));
        mvc.perform(get("/v1/chesscom/archive/meta")
                        .param("user","testuser").param("year","2024").param("month","1")
                        .with(TestAuth.jwtUser()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.count").value(2))
                .andExpect(jsonPath("$.timeControlDist.rapid").value(1));
    }

    @Test
    void ingest_ok() throws Exception {
        WireMock.stubFor(WireMock.get(WireMock.urlPathEqualTo("/pub/player/testuser/games/2024/01/pgn"))
                .willReturn(WireMock.ok("pgn")));
        String body = "{\"user\":\"testuser\",\"months\":[\"2024-01\"],\"datasetId\":null,\"note\":null}";
        mvc.perform(post("/v1/ingest/chesscom").with(TestAuth.jwtUser())
                .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.runId").exists());
    }
}
