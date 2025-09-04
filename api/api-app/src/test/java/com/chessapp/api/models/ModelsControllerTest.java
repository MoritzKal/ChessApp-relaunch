package com.chessapp.api.models;

import com.chessapp.api.models.api.ModelsController;
import com.chessapp.api.models.service.ModelRegistryService;
import com.chessapp.api.testutil.TestAuth;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {"logging.config=classpath:logback-spring.xml"},
        classes = com.chessapp.api.codex.CodexApplication.class)
@AutoConfigureMockMvc
class ModelsControllerTest extends com.chessapp.api.testutil.AbstractIntegrationTest {

    @Autowired MockMvc mvc;
    @MockBean ModelRegistryService service;

    @Test
    void count_ok() throws Exception {
        when(service.countActiveModels()).thenReturn(5L);
        mvc.perform(get("/v1/models/count").with(TestAuth.jwtUser()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.count").value(5));
    }
}
