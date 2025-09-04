package com.chessapp.api.datasets;

import com.chessapp.api.testutil.AbstractIntegrationTest;
import com.chessapp.api.testutil.TestAuth;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.notNullValue;
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

    @Test
    void sample_cursor() throws Exception {
        mvc.perform(get("/v1/datasets/abc/sample").with(TestAuth.jwtUser()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nextCursor").value("next"));
        mvc.perform(get("/v1/datasets/abc/sample?cursor=next").with(TestAuth.jwtUser()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nextCursor").doesNotExist());
    }

    @Test
    void export_redirect() throws Exception {
        mvc.perform(get("/v1/datasets/abc/export?format=csv").with(TestAuth.jwtUser()))
                .andExpect(status().is3xxRedirection())
                .andExpect(header().string("Location", notNullValue()));
    }
}
