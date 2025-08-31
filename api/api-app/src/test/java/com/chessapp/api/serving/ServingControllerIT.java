package com.chessapp.api.serving;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import com.chessapp.api.serving.dto.ModelsLoadRequest;
import com.chessapp.api.serving.dto.PredictRequest;
import com.chessapp.api.serving.dto.PredictResponse;
import com.chessapp.api.testutil.AbstractIntegrationTest;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    classes = com.chessapp.api.codex.CodexApplication.class)
class ServingControllerIT extends AbstractIntegrationTest {

    static MockWebServer serve = new MockWebServer();

    @DynamicPropertySource
    static void serveProps(DynamicPropertyRegistry registry) {
        registry.add("chs.serve.baseUrl", () -> serve.url("/").toString());
    }

    @BeforeAll
    static void start() throws IOException {
        serve.start();
    }

    @AfterAll
    static void stop() throws IOException {
        serve.shutdown();
    }

    @Autowired
    TestRestTemplate rest;

    @Autowired
    MeterRegistry meterRegistry;

    @Test
    void predict_happy_path() throws InterruptedException {
        serve.enqueue(new MockResponse()
                .setBody("{\"move\":\"e2e4\",\"legal\":[\"e2e4\"],\"modelId\":\"dummy\",\"modelVersion\":\"0\"}")
                .addHeader("Content-Type", "application/json"));

        ResponseEntity<PredictResponse> resp = rest.postForEntity("/v1/predict",
                new PredictRequest("some"), PredictResponse.class);

        assertThat(resp.getStatusCode().value()).isEqualTo(200);
        assertThat(resp.getBody()).isNotNull();
        assertThat(resp.getBody().move()).isEqualTo("e2e4");

        RecordedRequest req = serve.takeRequest();
        assertThat(req.getHeader("X-Run-Id")).isNotBlank();
        assertThat(req.getHeader("X-Username")).isEqualTo("M3NG00S3");
        assertThat(req.getHeader("X-Component")).isEqualTo("serve");

        Counter c = meterRegistry.find("chs_predict_requests_total")
                .tags("username", "M3NG00S3", "model_id", "dummy", "status", "ok")
                .counter();
        assertThat(c).isNotNull();
        assertThat(c.count()).isEqualTo(1.0);
    }

    @Test
    void predict_error_path() throws InterruptedException {
        serve.enqueue(new MockResponse().setResponseCode(400));
        ResponseEntity<String> resp = rest.postForEntity("/v1/predict",
                new PredictRequest("bad"), String.class);
        assertThat(resp.getStatusCode().value()).isEqualTo(400);
        // Drain the request so following tests don't read this one
        serve.takeRequest();
    }

    @Test
    void models_load_proxy_ok() throws InterruptedException {
        serve.enqueue(new MockResponse()
                .setBody("{\"modelId\":\"dummy\",\"modelVersion\":\"0\"}")
                .addHeader("Content-Type", "application/json"));

        ResponseEntity<String> resp = rest.postForEntity("/v1/models/load",
                new ModelsLoadRequest("dummy", null), String.class);
        assertThat(resp.getStatusCode().value()).isEqualTo(200);

        RecordedRequest req = serve.takeRequest();
        assertThat(req.getPath()).isEqualTo("/models/load");
    }

    @Test
    void models_load_proxy_error() throws InterruptedException {
        serve.enqueue(new MockResponse().setResponseCode(404));
        ResponseEntity<String> resp = rest.postForEntity("/v1/models/load",
                new ModelsLoadRequest("missing", "v1"), String.class);
        assertThat(resp.getStatusCode().value()).isEqualTo(404);
        // drain request
        serve.takeRequest();
    }
}
