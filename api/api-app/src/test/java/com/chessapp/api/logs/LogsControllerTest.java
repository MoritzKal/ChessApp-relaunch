package com.chessapp.api.logs;

import com.chessapp.api.metrics.service.ObsProxyClient;
import com.chessapp.api.testutil.AbstractIntegrationTest;
import com.chessapp.api.testutil.TestAuth;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
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
class LogsControllerTest extends AbstractIntegrationTest {

    @Autowired MockMvc mvc;
    @MockitoBean ObsProxyClient obs;

    @Test
    void training_logs_ok() throws Exception {
        Map<String,Object> loki = Map.of("data", Map.of("result", List.of(Map.of(
                "stream", Map.of("level","INFO"),
                "values", List.of(List.of("1","hello"))
        ))));
        when(obs.lokiRange(any(), any(Long.class), any(Long.class), any(Integer.class), any())).thenReturn(loki);
        mvc.perform(get("/v1/logs/training/abc").with(TestAuth.jwtUser()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].msg").value("hello"));
    }
}
