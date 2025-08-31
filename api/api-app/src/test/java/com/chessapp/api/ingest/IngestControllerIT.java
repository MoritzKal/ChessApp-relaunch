package com.chessapp.api.ingest;

import com.chessapp.api.codex.CodexApplication;
import com.chessapp.api.testutil.AbstractIntegrationTest;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;

import java.time.Instant;
import java.util.UUID;
import java.util.function.LongSupplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(
    classes = CodexApplication.class,
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = {
        "logging.config=classpath:logback-spring.xml",
        "management.prometheus.metrics.export.enabled=true"
    }
)
@AutoConfigureMockMvc
class IngestControllerIT extends AbstractIntegrationTest {

    @Autowired
    MockMvc mockMvc;

    @TestConfiguration
    static class TestCfg {
        @Bean
        LongSupplier ingestDelaySupplier() { return () -> 0L; }
    }

    @Test
    void ingest_flow_and_metrics() throws Exception {
        // 1) start ingest
        MvcResult start = mockMvc.perform(post("/v1/ingest")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"alice\"}"))
            .andExpect(status().isAccepted())
            .andReturn();
        String startBody = start.getResponse().getContentAsString();
        UUID runId = UUID.fromString(JsonPath.read(startBody, "$.runId"));

        // 2) poll status until SUCCEEDED within 2s
        Instant deadline = Instant.now().plusSeconds(2);
        String status = null;
        String reportUri = null;
        while (Instant.now().isBefore(deadline)) {
            MvcResult poll = mockMvc.perform(get("/v1/ingest/{id}", runId))
                    .andExpect(status().isOk())
                    .andReturn();
            String body = poll.getResponse().getContentAsString();
            status = JsonPath.read(body, "$.status");
            if ("SUCCEEDED".equals(status)) {
                reportUri = JsonPath.read(body, "$.reportUri");
                break;
            }
            Thread.sleep(50);
        }
        assertThat(status).isEqualTo("SUCCEEDED");
        assertThat(reportUri).isEqualTo("s3://reports/ingest/" + runId + "/report.json");

        // 3) alias redirect
        mockMvc.perform(post("/v1/data/import"))
                .andExpect(status().isPermanentRedirect())
                .andExpect(header().string(HttpHeaders.LOCATION, "/v1/ingest"));

        // 4) prometheus metrics contain started counter > 0
        MvcResult prom = mockMvc.perform(get("/actuator/prometheus"))
                .andExpect(status().isOk())
                .andReturn();
        String promBody = prom.getResponse().getContentAsString();
        Pattern pattern = Pattern.compile("chs_ingest_runs_started_total\\{[^}]*} (\\d+(?:\\.\\d+)?)");
        Matcher matcher = pattern.matcher(promBody);
        assertThat(matcher.find()).isTrue();
        assertThat(Double.parseDouble(matcher.group(1))).isGreaterThan(0.0);
    }
}

