package com.chessapp.api.datasets;

import com.chessapp.api.service.DatasetService;
import com.chessapp.api.testutil.AbstractIntegrationTest;
import com.chessapp.api.testutil.TestAuth;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.boot.test.mock.mockito.MockBean;

import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {"logging.config=classpath:logback-spring.xml"},
        classes = com.chessapp.api.codex.CodexApplication.class)
@AutoConfigureMockMvc
class DatasetsControllerTest extends AbstractIntegrationTest {

    @Autowired MockMvc mvc;
    @MockBean DatasetService service;

    @Test
    void summary_ok() throws Exception {
        var id = java.util.UUID.randomUUID();
        var resp = new com.chessapp.api.service.dto.DatasetResponse(
                id, "ds", "v1", 0L, "uri", java.time.Instant.now());
        org.mockito.Mockito.when(service.get(id)).thenReturn(resp);
        mvc.perform(get("/v1/datasets/" + id + "/summary").with(TestAuth.jwtUser()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id.toString()));
    }

    @Test
    void sample_cursor() throws Exception {
        var id = java.util.UUID.randomUUID();
        mvc.perform(get("/v1/datasets/" + id + "/sample").with(TestAuth.jwtUser()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nextCursor").value("next"));
        mvc.perform(get("/v1/datasets/" + id + "/sample?cursor=next").with(TestAuth.jwtUser()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nextCursor").doesNotExist());
    }

    @Test
    void export_redirect() throws Exception {
        var id = java.util.UUID.randomUUID();
        mvc.perform(get("/v1/datasets/" + id + "/export?format=csv").with(TestAuth.jwtUser()))
                .andExpect(status().is3xxRedirection())
                .andExpect(header().string("Location", notNullValue()));
    }
}
