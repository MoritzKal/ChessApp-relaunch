package com.chessapp.api.training;

import com.chessapp.api.training.api.TrainingStartRequest;
import com.chessapp.api.training.service.MlClient;
import com.chessapp.api.testutil.AbstractIntegrationTest;
import com.chessapp.api.testutil.TestAuth;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import com.chessapp.api.domain.entity.TrainingRun;
import com.chessapp.api.domain.entity.TrainingStatus;
import com.chessapp.api.domain.repo.TrainingRunRepository;

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

    @MockitoBean TrainingRunRepository repo;
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
        when(repo.findAllByOrderByStartedAtDesc(any())).thenReturn(new ArrayList<>());
    }

    @Test
    void start_and_status_ok() throws Exception {
        var req = new TrainingStartRequest(UUID.randomUUID(), "v1", UUID.randomUUID(), 2, 3, 1e-3, "sgd", 1, null, true, "normal");
        String body = om.writeValueAsString(req);
        MvcResult start = mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/v1/trainings")
                        .with(TestAuth.jwtUser())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isCreated())
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

    @Test
    void start_invalid_epochs() throws Exception {
        var req = new TrainingStartRequest(UUID.randomUUID(), "v1", UUID.randomUUID(), 0, 1, 0.1, "adam", 1, null, true, "normal");
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/v1/trainings")
                        .with(TestAuth.jwtUser())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(req)))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isBadRequest());
    }

    @Test
    void control_ok_and_bad_request() throws Exception {
        UUID runId = UUID.randomUUID();
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/v1/trainings/{id}/control", runId)
                        .with(TestAuth.jwtUser())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"action\":\"pause\"}"))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isAccepted());

        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/v1/trainings/{id}/control", runId)
                        .with(TestAuth.jwtUser())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"action\":\"foo\"}"))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isBadRequest());
    }

    @Test
    void list_ok() throws Exception {
        var runs = new ArrayList<TrainingRun>();
        for (int i = 0; i < 3; i++) {
            TrainingRun tr = new TrainingRun();
            tr.setId(UUID.randomUUID());
            tr.setStatus(TrainingStatus.RUNNING);
            tr.setStartedAt(Instant.now());
            runs.add(tr);
        }
        when(repo.findAllByOrderByStartedAtDesc(any())).thenReturn(runs);
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/v1/trainings")
                        .with(TestAuth.jwtUser())
                        .param("limit", "20"))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isOk())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.items.length()").value(3));
    }
}
