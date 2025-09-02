package com.chessapp.api.it;

import com.chessapp.api.codex.CodexApplication;
import com.chessapp.api.support.JwtTestUtils;
import com.chessapp.api.testutil.AbstractIntegrationTest;
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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = CodexApplication.class)
@AutoConfigureMockMvc
@ActiveProfiles({"codex", "test"})
@ExtendWith(OutputCaptureExtension.class)
class LoggingIT extends AbstractIntegrationTest {

    @Autowired
    MockMvc mvc;

    @Value("${app.security.jwt.secret}")
    String secret;

    @RestController
    static class TestController {
        private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(TestController.class);

        @GetMapping("/v1/logging-test")
        String logSomething() {
            log.info("logging.test");
            return "ok";
        }
    }

    @Test
    void mdc_fields_are_logged(CapturedOutput output) throws Exception {
        String token = JwtTestUtils.signHmac256(secret, Map.of(
                "preferred_username", "alice",
                "roles", List.of("USER")
        ), Duration.ofMinutes(5));

        mvc.perform(get("/v1/logging-test")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .header("X-Run-Id", "r123")
                        .header("X-Dataset-Id", "d456")
                        .header("X-Model-Id", "m789"))
                .andExpect(status().isOk());

        boolean found = Arrays.stream(output.getOut().split("\n"))
                .anyMatch(line -> line.contains("\"run_id\":\"r123\"")
                        && line.contains("\"dataset_id\":\"d456\"")
                        && line.contains("\"model_id\":\"m789\"")
                        && line.contains("\"username\":\"alice\"")
                        && line.contains("\"component\":\"api\""));
        assertThat(found).isTrue();
    }
}
