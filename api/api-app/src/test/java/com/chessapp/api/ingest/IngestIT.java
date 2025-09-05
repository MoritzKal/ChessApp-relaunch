package com.chessapp.api.ingest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.matchesPattern;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import com.chessapp.api.codex.CodexApplication;
import com.chessapp.api.support.JwtTestUtils;
import com.chessapp.api.testutil.AbstractIntegrationTest;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.boot.test.mock.mockito.MockBean;
import software.amazon.awssdk.services.s3.S3Client;

/**
 * Integration tests for ingest endpoint.
 */
@ExtendWith(OutputCaptureExtension.class)
@SpringBootTest(classes = CodexApplication.class)
@AutoConfigureMockMvc
@ActiveProfiles({"codex", "test"})
class IngestIT extends AbstractIntegrationTest {

    @Autowired
    MockMvc mvc;

    @MockBean S3Client s3;

    @Value("${app.security.jwt.secret}")
    String secret;

    private String authHeader() {
        String token = JwtTestUtils.signHmac256(secret, Map.of(
                "preferred_username", "user1",
                "roles", List.of("USER", "MONITORING"),
                "scope", "api.write"),
                Duration.ofMinutes(5));
        return "Bearer " + token;
    }

    @Test
    void start_poll_success_and_metrics(CapturedOutput output) throws Exception {
        String auth = authHeader();
        ObjectMapper mapper = new ObjectMapper();

        MvcResult start = mvc.perform(post("/v1/ingest")
                        .header(HttpHeaders.AUTHORIZATION, auth))
                .andExpect(status().isAccepted())
                .andExpect(header().string(HttpHeaders.LOCATION, matchesPattern("/v1/ingest/.*")))
                .andExpect(jsonPath("$.runId").exists())
                .andReturn();

        JsonNode startBody = mapper.readTree(start.getResponse().getContentAsString());
        String runId = startBody.get("runId").asText();

        String statusVal = null;
        String reportUri = null;
        for (int i = 0; i < 20; i++) {
            MvcResult st = mvc.perform(get("/v1/ingest/{id}", runId)
                            .header(HttpHeaders.AUTHORIZATION, auth))
                    .andReturn();
            JsonNode body = mapper.readTree(st.getResponse().getContentAsString());
            statusVal = body.get("status").asText();
            if (body.hasNonNull("reportUri")) {
                reportUri = body.get("reportUri").asText();
            }
            if ("SUCCEEDED".equals(statusVal) || "FAILED".equals(statusVal)) {
                break;
            }
            Thread.sleep(500);
        }
        assertThat(statusVal).isEqualTo("SUCCEEDED");
        assertThat(reportUri).matches("s3://reports/ingest/" + runId + "/report\\.json");

        MvcResult metrics = mvc.perform(get("/actuator/prometheus")
                        .header(HttpHeaders.AUTHORIZATION, auth))
                .andExpect(status().isOk())
                .andReturn();
        String metricsBody = metrics.getResponse().getContentAsString();
        assertThat(metricsBody).contains("chs_ingest_starts_total");
        assertThat(metricsBody).contains("chs_ingest_duration_seconds_count");

        boolean foundLog = Arrays.stream(output.getOut().split("\n"))
                .anyMatch(l -> l.contains("\"run_id\":\"" + runId + "\""));
        assertThat(foundLog).isTrue();
    }

    @Test
    void legacy_alias_redirects() throws Exception {
        String auth = authHeader();
        mvc.perform(post("/v1/data/import")
                        .header(HttpHeaders.AUTHORIZATION, auth))
                .andExpect(status().isPermanentRedirect())
                .andExpect(header().string(HttpHeaders.LOCATION, "/v1/ingest"));
    }
}

