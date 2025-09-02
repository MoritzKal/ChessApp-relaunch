package com.chessapp.api.training;

import com.chessapp.api.training.api.TrainingStartRequest;
import com.chessapp.api.training.service.MlClient;
import com.chessapp.api.testutil.AbstractIntegrationTest;
import com.chessapp.api.testutil.TestAuth;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import com.chessapp.api.domain.entity.TrainingRun;
import com.chessapp.api.domain.repo.TrainingRunRepository;
import java.util.Optional;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@SpringBootTest(
        properties = {"logging.config=classpath:logback-spring.xml"},
        classes = com.chessapp.api.codex.CodexApplication.class
)
@AutoConfigureMockMvc
class TrainingControllerIT extends AbstractIntegrationTest {

    @TestConfiguration
    static class Cfg {
        @Bean
        @Primary
        MlClient fake() {
            return new MlClient() {
                @Override public void postTrain(UUID runId, UUID datasetId, Map<String,Object> params) { /* no-op */ }
                @Override public Map<String, Object> getRun(UUID runId) {
                    return Map.of("status","succeeded","metrics", Map.of("loss",0.1,"val_acc",0.9));
                }
            };
        }
    }

    @MockBean TrainingRunRepository repo;
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper om;

    Map<UUID, TrainingRun> store;

    @BeforeEach
    void setupRepo() {
        store = new ConcurrentHashMap<>();
        when(repo.save(any())).thenAnswer(invocation -> {
            TrainingRun tr = invocation.getArgument(0);
            store.put(tr.getId(), tr);
            return tr;
        });
        when(repo.findById(any())).thenAnswer(invocation -> Optional.ofNullable(store.get(invocation.getArgument(0))));
    }

    @Test
    void start_and_status_ok() throws Exception {
        var req = new TrainingStartRequest(null, "policy_tiny", Map.of("epochs",2,"stepsPerEpoch",3,"lr",1e-3));
        String body = om.writeValueAsString(req);
        MvcResult start = mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/v1/trainings")
                        .with(TestAuth.jwtUser())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isAccepted())
                .andReturn();

        String runId = String.valueOf(om.readTree(start.getResponse().getContentAsString()).get("runId").asText());

        MvcResult status = mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/v1/trainings/{id}", runId)
                        .with(TestAuth.jwtUser()))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isOk())
                .andReturn();

        Map<?,?> resp = om.readValue(status.getResponse().getContentAsString(), Map.class);
        org.assertj.core.api.Assertions.assertThat(resp.get("status")).isEqualTo("succeeded");
        @SuppressWarnings("unchecked")
        Map<String,Object> metrics = (Map<String,Object>) resp.get("metrics");
        org.assertj.core.api.Assertions.assertThat(metrics).isNotNull();
        org.assertj.core.api.Assertions.assertThat(metrics.containsKey("loss")).isTrue();
        org.assertj.core.api.Assertions.assertThat(metrics.containsKey("val_acc")).isTrue();
    }
}
