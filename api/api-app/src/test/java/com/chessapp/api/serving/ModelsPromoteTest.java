package com.chessapp.api.serving;

import com.chessapp.api.testutil.TestAuth;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.http.MediaType;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ServingController.class)
class ModelsPromoteTest {

    @Autowired
    MockMvc mvc;

    @MockitoBean
    ServingClient client;

    @Test
    void promote() throws Exception {
        mvc.perform(post("/v1/models/promote").with(TestAuth.jwtUser())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"modelId\":\"m1\",\"targetStage\":\"prod\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ok").value(true));
    }
}
