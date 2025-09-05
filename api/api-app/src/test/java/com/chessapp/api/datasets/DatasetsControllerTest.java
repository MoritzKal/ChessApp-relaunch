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
import software.amazon.awssdk.services.s3.S3Client;

import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {"logging.config=classpath:logback-spring.xml"},
        classes = com.chessapp.api.codex.CodexApplication.class)
@AutoConfigureMockMvc
class DatasetsControllerTest extends AbstractIntegrationTest {

    @Autowired MockMvc mvc;
    @MockBean DatasetService service;
    @MockBean S3Client s3;

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

    @Test
    void get_not_found() throws Exception {
        org.mockito.Mockito.when(service.getByName("does_not_exist"))
                .thenThrow(new jakarta.persistence.EntityNotFoundException());
        mvc.perform(get("/v1/datasets/does_not_exist").with(TestAuth.jwtUser()))
                .andExpect(status().isNotFound());
    }
}
