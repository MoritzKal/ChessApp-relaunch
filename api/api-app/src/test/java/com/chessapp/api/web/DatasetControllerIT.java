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

import java.util.UUID;

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

    @Test
    void get_by_uuid_ok() throws Exception {
        UUID id = UUID.randomUUID();
        var resp = new com.chessapp.api.service.dto.DatasetResponse(
                id, "ds", "v1", 0L, "uri", java.time.Instant.now());
        when(service.get(id)).thenReturn(resp);
        mvc.perform(get("/v1/datasets/" + id).with(TestAuth.jwtUser()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id.toString()));
    }

    @Test
    void get_by_name_ok() throws Exception {
        UUID id = UUID.randomUUID();
        var resp = new com.chessapp.api.service.dto.DatasetResponse(
                id, "sample", "v1", 0L, "uri", java.time.Instant.now());
        when(service.getByName("sample")).thenReturn(resp);
        mvc.perform(get("/v1/datasets/sample").with(TestAuth.jwtUser()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("sample"));
    }

    @Test
    void get_not_found() throws Exception {
        UUID id = UUID.randomUUID();
        when(service.get(id)).thenThrow(new jakarta.persistence.EntityNotFoundException());
        mvc.perform(get("/v1/datasets/" + id).with(TestAuth.jwtUser()))
                .andExpect(status().isNotFound());
    }
}
