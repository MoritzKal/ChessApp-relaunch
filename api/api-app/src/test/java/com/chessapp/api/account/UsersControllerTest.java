package com.chessapp.api.account;

import com.chessapp.api.testutil.TestAuth;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.http.MediaType;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(UsersController.class)
class UsersControllerTest {

    @Autowired
    MockMvc mvc;

    @Test
    void me() throws Exception {
        mvc.perform(get("/v1/users/me").with(TestAuth.jwtUser()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("test-user"));
    }

    @Test
    void prefs() throws Exception {
        String body = "{\"temperature\":0.7,\"topk\":5,\"boardOrientation\":\"black\",\"useGameAsTraining\":false}";
        mvc.perform(put("/v1/users/me/prefs").contentType(MediaType.APPLICATION_JSON).content(body).with(TestAuth.jwtUser()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.topk").value(5));
        mvc.perform(get("/v1/users/me/prefs").with(TestAuth.jwtUser()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.boardOrientation").value("black"));
    }
}
