package com.chessapp.api.evaluations;

import com.chessapp.api.testutil.TestAuth;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(EvaluationsController.class)
class EvaluationsControllerTest {

    @Autowired
    MockMvc mvc;

    @Test
    void createAndGet() throws Exception {
        String json = mvc.perform(post("/v1/evaluations").contentType(MediaType.APPLICATION_JSON)
                .content("{\"baselineModelId\":\"b\",\"candidateModelId\":\"c\",\"suite\":\"accuracy\"}")
                .with(TestAuth.jwtUser()))
                .andExpect(status().isAccepted())
                .andReturn().getResponse().getContentAsString();
        String id = new com.fasterxml.jackson.databind.ObjectMapper().readTree(json).get("evaluationId").asText();
        mvc.perform(get("/v1/evaluations/" + id).with(TestAuth.jwtUser()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.summary.baseline").value("b"));
    }

    @Test
    void notFound() throws Exception {
        mvc.perform(get("/v1/evaluations/doesnotexist").with(TestAuth.jwtUser()))
                .andExpect(status().isNotFound());
    }
}
