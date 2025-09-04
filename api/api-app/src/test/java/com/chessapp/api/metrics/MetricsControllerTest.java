package com.chessapp.api.metrics;

import com.chessapp.api.metrics.service.ObsProxyClient;
import com.chessapp.api.testutil.AbstractIntegrationTest;
import com.chessapp.api.testutil.TestAuth;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {"logging.config=classpath:logback-spring.xml"},
        classes = com.chessapp.api.codex.CodexApplication.class)
@AutoConfigureMockMvc
class MetricsControllerTest extends AbstractIntegrationTest {

    @Autowired MockMvc mvc;
    @MockBean ObsProxyClient obs;

    private Map<String,Object> prom(double v) {
        return Map.of("data", Map.of("result", List.of(Map.of("values", List.of(List.of(1, String.valueOf(v)))))));
    }

    @Test
    void rps_ok() throws Exception {
        when(obs.promRange(any(), any(Long.class), any(Long.class), any())).thenReturn(prom(1.0));
        mvc.perform(get("/v1/metrics/rps").with(TestAuth.jwtUser()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.series[0].label").value("rps"));
    }

    @Test
    void training_ok() throws Exception {
        when(obs.promRange(any(), any(Long.class), any(Long.class), any())).thenReturn(prom(0.5));
        mvc.perform(get("/v1/metrics/training/abc").param("m","loss,val_acc").with(TestAuth.jwtUser()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.series[0].label").value("loss"));
    }
}
