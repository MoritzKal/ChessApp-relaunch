package com.chessapp.api.ingest;

import com.chessapp.api.testutil.AbstractIntegrationTest;
import com.chessapp.api.testutil.TestAuth;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {"logging.config=classpath:logback-spring.xml"},
        classes = com.chessapp.api.codex.CodexApplication.class)
@AutoConfigureMockMvc
class IngestControllerTest extends AbstractIntegrationTest {

    @Autowired MockMvc mvc;

    @Test
    void start_ok() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "data.txt", MediaType.TEXT_PLAIN_VALUE, "hi".getBytes());
        mvc.perform(multipart("/v1/ingest").file(file).with(TestAuth.jwtUser()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.runId").exists())
                .andExpect(jsonPath("$.status").value("queued"));
    }

    @Test
    void status_not_found() throws Exception {
        mvc.perform(get("/v1/ingest/{id}", UUID.randomUUID()).with(TestAuth.jwtUser()))
                .andExpect(status().isNotFound());
    }
}
