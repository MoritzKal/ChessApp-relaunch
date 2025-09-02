package com.chessapp.api.filter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@SpringBootTest
@AutoConfigureMockMvc
class IdempotencyFilterTest {

    @RestController
    static class EchoController {
        @PostMapping(path = "/v1/test", produces = MediaType.APPLICATION_JSON_VALUE)
        public String echo(@RequestBody String body) {
            return body;
        }
    }

    @Autowired
    MockMvc mvc;

    @Test
    void replayReturnsCachedResponse() throws Exception {
        String payload = "{\"a\":1}";
        String key = "abc123";

        String first = mvc.perform(post("/v1/test").contentType(MediaType.APPLICATION_JSON).content(payload).header("Idempotency-Key", key))
                .andExpect(status().isOk())
                .andExpect(header().string("Idempotent-Replay", "false"))
                .andReturn().getResponse().getContentAsString();

        String second = mvc.perform(post("/v1/test").contentType(MediaType.APPLICATION_JSON).content(payload).header("Idempotency-Key", key))
                .andExpect(status().isOk())
                .andExpect(header().string("Idempotent-Replay", "true"))
                .andReturn().getResponse().getContentAsString();

        assertThat(second).isEqualTo(first);
    }
}
