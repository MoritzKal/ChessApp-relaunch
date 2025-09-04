package com.chessapp.api.play;

import com.chessapp.api.testutil.TestAuth;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import com.fasterxml.jackson.databind.ObjectMapper;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(PlayController.class)
class PlayControllerTest {

    @Autowired
    MockMvc mvc;

    @Autowired
    ObjectMapper mapper;

    @Test
    void startAndGet() throws Exception {
        String json = mvc.perform(post("/v1/play/new").with(TestAuth.jwtUser()))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        String gameId = mapper.readTree(json).get("gameId").asText();
        mvc.perform(get("/v1/play/" + gameId).with(TestAuth.jwtUser()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fen").value("startpos"));
    }

    @Test
    void moveUnknown() throws Exception {
        mvc.perform(post("/v1/play/doesnotexist/move").contentType(MediaType.APPLICATION_JSON)
                .content("{\"uci\":\"e2e4\"}").with(TestAuth.jwtUser()))
                .andExpect(status().isNotFound());
    }
}
