package com.chessapp.api.datasets;

import com.chessapp.api.testutil.AbstractIntegrationTest;
import com.chessapp.api.testutil.TestAuth;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {"logging.config=classpath:logback-spring.xml"},
        classes = com.chessapp.api.codex.CodexApplication.class)
@AutoConfigureMockMvc
class DatasetsControllerTest extends AbstractIntegrationTest {

    @Autowired MockMvc mvc;

    @Test
    void summary_ok() throws Exception {
        mvc.perform(get("/v1/datasets/abc/summary").with(TestAuth.jwtUser()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("abc"));
    }
}
