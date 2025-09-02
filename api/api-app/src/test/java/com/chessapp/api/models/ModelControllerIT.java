package com.chessapp.api.models;

import com.chessapp.api.domain.entity.Model;
import com.chessapp.api.domain.repo.ModelRepository;
import com.chessapp.api.testutil.AbstractIntegrationTest;
import com.chessapp.api.testutil.TestAuth;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(classes = com.chessapp.api.codex.CodexApplication.class)
@AutoConfigureMockMvc
@ActiveProfiles("codex")
class ModelControllerIT extends AbstractIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired ModelRepository repo;

    UUID m1; UUID m2; UUID m3;

    @BeforeEach
    void setup() {
        repo.deleteAll();
        m1 = insert("m1", false);
        m2 = insert("m2", false);
        m3 = insert("m3", true);
    }

    private UUID insert(String name, boolean prod) {
        Model m = new Model();
        m.setId(UUID.randomUUID());
        m.setName(name);
        m.setVersion("v1");
        m.setFramework("tf");
        m.setMetrics(Map.of());
        m.setArtifactUri("uri");
        m.setCreatedAt(Instant.now());
        m.setProd(prod);
        return repo.save(m).getId();
    }

    @Test
    void listModels_returnsIsProd() throws Exception {
        mvc.perform(get("/v1/models").with(TestAuth.jwtUser()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(3)))
                .andExpect(jsonPath("$[?(@.id=='"+m3.toString()+"')].isProd", contains(true)));
    }

    @Test
    void getModel_returnsModel() throws Exception {
        mvc.perform(get("/v1/models/"+m1).with(TestAuth.jwtUser()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(m1.toString())));
    }

    @Test
    void promote_setsOnlyOneProd() throws Exception {
        mvc.perform(post("/v1/models/promote")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"modelId\":\""+m1+"\"}")
                        .with(TestAuth.jwtUser()))
                .andExpect(status().isOk());
        mvc.perform(get("/v1/models").with(TestAuth.jwtUser()))
                .andExpect(jsonPath("$[?(@.id=='"+m1+"')].isProd", contains(true)))
                .andExpect(jsonPath("$[?(@.id=='"+m3+"')].isProd", contains(false)));
    }

    @Test
    void promote_twice_recordsNoopMetric() throws Exception {
        mvc.perform(post("/v1/models/promote")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"modelId\":\""+m3+"\"}")
                        .with(TestAuth.jwtUser()))
                .andExpect(status().isOk());
        mvc.perform(post("/v1/models/promote")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"modelId\":\""+m3+"\"}")
                        .with(TestAuth.jwtUser()))
                .andExpect(status().isOk());
        mvc.perform(get("/actuator/prometheus").with(TestAuth.jwtMonitoring()))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("chs_model_promotions_total{result=\"noop\"}")));
    }

    @Test
    void promote_requiresAuth() throws Exception {
        mvc.perform(post("/v1/models/promote")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"modelId\":\""+m1+"\"}"))
                .andExpect(status().isUnauthorized());
    }
}
