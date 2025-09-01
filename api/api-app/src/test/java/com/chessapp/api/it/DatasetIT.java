package com.chessapp.api.it;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.time.Duration;
import java.util.List;
import java.util.Map;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.chessapp.api.codex.CodexApplication;
import com.chessapp.api.service.DatasetService;
import com.chessapp.api.support.JwtTestUtils;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        classes = CodexApplication.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
class DatasetIT {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    @Value("${app.security.jwt.secret}")
    String secret;

    @Test
    void create_get_list_and_logs() throws Exception {
        String token = JwtTestUtils.signHmac256(secret, Map.of(
                "preferred_username", "user1",
                "roles", List.of("USER"),
                "scope", "api.write"), Duration.ofMinutes(5));

        Logger logger = (Logger) org.slf4j.LoggerFactory.getLogger(DatasetService.class);
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);

        String payload = """
            {"name":"ds1","version":"v1","filter":{"src":"x"},"split":{"train":1.0},"sizeRows":1}
            """;

        MvcResult result = mockMvc.perform(post("/v1/datasets")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isCreated())
                .andExpect(header().string(HttpHeaders.LOCATION, org.hamcrest.Matchers.matchesPattern(".*/v1/datasets/.*")))
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andReturn();

        JsonNode node = objectMapper.readTree(result.getResponse().getContentAsString());
        String id = node.get("id").asText();
        String location = result.getResponse().getHeader(HttpHeaders.LOCATION);
        assertThat(location).endsWith("/v1/datasets/" + id);

        boolean logHasDatasetId = appender.list.stream()
                .anyMatch(ev -> id.equals(ev.getMDCPropertyMap().get("dataset_id"))
                        && "api".equals(ev.getMDCPropertyMap().get("component"))
                        && "user1".equals(ev.getMDCPropertyMap().get("username")));
        assertThat(logHasDatasetId).isTrue();
        logger.detachAppender(appender);

        mockMvc.perform(get("/v1/datasets/" + id)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id));

        mockMvc.perform(get("/v1/datasets")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(id));

        mockMvc.perform(post("/v1/datasets")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }
}
