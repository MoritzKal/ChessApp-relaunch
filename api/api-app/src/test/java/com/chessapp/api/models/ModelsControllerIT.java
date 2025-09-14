package com.chessapp.api.models;

import com.chessapp.api.support.JwtTestUtils;
import com.chessapp.api.testutil.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Duration;
import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import com.chessapp.api.testutil.TestAuth;

@SpringBootTest(classes = com.chessapp.api.codex.CodexApplication.class)
@AutoConfigureMockMvc
@ActiveProfiles("codex")
class ModelsControllerIT extends AbstractIntegrationTest {
  @Autowired MockMvc mvc;
  @Value("${app.security.jwt.secret}") String secret;

  @SuppressWarnings("unused")
  private String authHeader() {
    String token = JwtTestUtils.signHmac256(secret, Map.of(
        "preferred_username", "user1",
        "roles", List.of("USER", "MONITORING"),
        "scope", "api.read"),
        Duration.ofMinutes(5));
    return "Bearer " + token;
  }

  @Test void listModels_returns200AndFields() throws Exception {
    mvc.perform(get("/v1/models").with(TestAuth.jwtUser()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].modelId", notNullValue()));
  }

  @Test void listVersions_returns200AndFields() throws Exception {
    mvc.perform(get("/v1/models/policy_tiny/versions").with(TestAuth.jwtUser()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].modelVersion", notNullValue()));
  }

  @Test void listVersions_unknownModel_returns404() throws Exception {
    mvc.perform(get("/v1/models/does_not_exist/versions").with(TestAuth.jwtUser()))
        .andExpect(status().isNotFound());
  }

  @Test void metrics_smoke_present_after_request() throws Exception {
    mvc.perform(get("/v1/models").with(TestAuth.jwtUser())).andExpect(status().isOk());
    mvc.perform(get("/actuator/prometheus").with(TestAuth.jwtMonitoring()))
        .andExpect(status().isOk())
        // Global metric tags (application, component, username, ...) are present,
        // so we assert key substrings rather than exact label set/order.
        .andExpect(content().string(containsString("chs_model_registry_requests_total{")))
        .andExpect(content().string(containsString("endpoint=\"/v1/models\"")))
        .andExpect(content().string(containsString("status=\"200\"")));
  }
}
