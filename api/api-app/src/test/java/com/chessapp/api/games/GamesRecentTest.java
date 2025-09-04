package com.chessapp.api.games;

import com.chessapp.api.games.api.GamesController;
import com.chessapp.api.service.GameService;
import com.chessapp.api.service.dto.GameSummaryDto;
import com.chessapp.api.testutil.TestAuth;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {"logging.config=classpath:logback-spring.xml"},
        classes = com.chessapp.api.codex.CodexApplication.class)
@AutoConfigureMockMvc
class GamesRecentTest extends com.chessapp.api.testutil.AbstractIntegrationTest {

    @Autowired MockMvc mvc;
    @MockBean GameService service;

    @Test
    void recent_ok() throws Exception {
        var g = new GameSummaryDto(UUID.randomUUID(), Instant.now(), "blitz", null, null, null);
        when(service.listRecent(50)).thenReturn(List.of(g));
        mvc.perform(get("/v1/games/recent").with(TestAuth.jwtUser()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").exists());
    }

    @Test
    void online_ok() throws Exception {
        mvc.perform(get("/v1/games/online_count").with(TestAuth.jwtUser()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.count").value(0));
    }
}
