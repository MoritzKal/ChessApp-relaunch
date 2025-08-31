package com.chessapp.api.models;

import com.chessapp.api.testutil.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(classes = com.chessapp.api.codex.CodexApplication.class)
@AutoConfigureMockMvc
@ActiveProfiles("codex")
class ModelsControllerIT extends AbstractIntegrationTest {
  @Autowired MockMvc mvc;

  @Test void listModels_returns200AndFields() throws Exception {
    mvc.perform(get("/v1/models"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].modelId", notNullValue()));
  }

  @Test void listVersions_returns200AndFields() throws Exception {
    mvc.perform(get("/v1/models/policy_tiny/versions"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].modelVersion", notNullValue()));
  }

  @Test void listVersions_unknownModel_returns404() throws Exception {
    mvc.perform(get("/v1/models/does_not_exist/versions"))
        .andExpect(status().isNotFound());
  }

  @Test void metrics_smoke_present_after_request() throws Exception {
    mvc.perform(get("/v1/models")).andExpect(status().isOk());
    mvc.perform(get("/actuator/prometheus"))
        .andExpect(status().isOk())
        // Global metric tags (application, component, username, ...) are present,
        // so we assert key substrings rather than exact label set/order.
        .andExpect(content().string(containsString("chs_model_registry_requests_total{")))
        .andExpect(content().string(containsString("endpoint=\"/v1/models\"")))
        .andExpect(content().string(containsString("status=\"200\"")));
  }
}
