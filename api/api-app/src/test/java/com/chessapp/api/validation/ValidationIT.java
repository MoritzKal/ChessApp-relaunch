package com.chessapp.api.validation;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import com.chessapp.api.testutil.AbstractIntegrationTest;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    classes = com.chessapp.api.codex.CodexApplication.class)
@AutoConfigureMockMvc
@ActiveProfiles("codex")
class ValidationIT extends AbstractIntegrationTest {

    @Autowired
    MockMvc mvc;

    @Test
    void pgn_valid_returns200() throws Exception {
        String pgn = "[Event \"?\"]\n\n1. e4 e5 2. Nf3 Nc6 3. Bb5 a6 *";
        mvc.perform(post("/test/pgn")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"pgn\":\"" + pgn + "\"}"))
                .andExpect(status().isOk());
    }

    @Test
    void pgn_invalid_returns400() throws Exception {
        mvc.perform(post("/test/pgn")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"pgn\":\"not a pgn\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("pgn"));
    }
}
