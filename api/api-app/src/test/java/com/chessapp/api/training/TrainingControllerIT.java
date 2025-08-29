package com.chessapp.api.training;

import com.chessapp.api.training.api.TrainingStartRequest;
import com.chessapp.api.training.service.MlClient;
import com.chessapp.api.training.service.TrainingService;
import com.chessapp.api.testutil.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Map;
import java.util.UUID;

class TrainingControllerIT extends AbstractIntegrationTest {

    @TestConfiguration
    static class Cfg {
        @Bean
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
    org.springframework.context.ApplicationContext ctx;

    @Test
    void start_and_status_ok() {
        WebTestClient client = WebTestClient.bindToApplicationContext(ctx).build();

        var req = new TrainingStartRequest(null, "policy_tiny", Map.of("epochs",2,"stepsPerEpoch",3,"lr",1e-3));
        String runId = client.post().uri("/v1/trainings")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(req)
                .exchange()
                .expectStatus().isAccepted()
                .expectBody()
                .jsonPath("$.runId").value(String::valueOf);

        client.get().uri("/v1/trainings/{id}", runId)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.status").isEqualTo("succeeded")
                .jsonPath("$.metrics.loss").exists()
                .jsonPath("$.metrics.val_acc").exists();
    }
}

