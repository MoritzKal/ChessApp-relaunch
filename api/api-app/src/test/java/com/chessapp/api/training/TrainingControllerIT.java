package com.chessapp.api.training;

import com.chessapp.api.training.api.TrainingStartRequest;
import com.chessapp.api.training.service.MlClient;
import com.chessapp.api.training.service.TrainingService;
import com.chessapp.api.testutil.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import java.util.Map;
import java.util.UUID;

@SpringBootTest(
        properties = {"logging.config=classpath:logback-spring.xml"},
        classes = com.chessapp.api.codex.CodexApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
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

    @Autowired
    TrainingService service;

    @Autowired
    TestRestTemplate rest;

    @Test
    void start_and_status_ok() {
        var req = new TrainingStartRequest(null, "policy_tiny", Map.of("epochs",2,"stepsPerEpoch",3,"lr",1e-3));
        var startResp = rest.postForEntity("/v1/trainings", req, Map.class);
        org.assertj.core.api.Assertions.assertThat(startResp.getStatusCode().value()).isEqualTo(202);
        @SuppressWarnings("unchecked")
        String runId = String.valueOf(((Map<String,Object>) java.util.Objects.requireNonNull(startResp.getBody())).get("runId"));

        var statusResp = rest.getForEntity("/v1/trainings/{id}", Map.class, runId);
        org.assertj.core.api.Assertions.assertThat(statusResp.getStatusCode().value()).isEqualTo(200);
        @SuppressWarnings("unchecked")
        Map<String,Object> body = (Map<String,Object>) java.util.Objects.requireNonNull(statusResp.getBody());
        org.assertj.core.api.Assertions.assertThat(body.get("status")).isEqualTo("succeeded");
        @SuppressWarnings("unchecked")
        Map<String,Object> metrics = (Map<String,Object>) body.get("metrics");
        org.assertj.core.api.Assertions.assertThat(metrics).isNotNull();
        org.assertj.core.api.Assertions.assertThat(metrics.containsKey("loss")).isTrue();
        org.assertj.core.api.Assertions.assertThat(metrics.containsKey("val_acc")).isTrue();
    }
}
