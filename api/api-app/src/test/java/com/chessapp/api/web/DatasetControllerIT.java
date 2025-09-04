package com.chessapp.api.web;

import com.chessapp.api.service.DatasetService;
import com.chessapp.api.testutil.AbstractIntegrationTest;
import com.chessapp.api.testutil.TestAuth;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {"logging.config=classpath:logback-spring.xml"},
        classes = com.chessapp.api.codex.CodexApplication.class)
@AutoConfigureMockMvc
class DatasetControllerIT extends AbstractIntegrationTest {

    @Autowired MockMvc mvc;
    @MockBean DatasetService service;

    @Test
    void count_ok() throws Exception {
        when(service.count()).thenReturn(3L);
        mvc.perform(get("/v1/datasets/count").with(TestAuth.jwtUser())
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.count").value(3));
    }
}
