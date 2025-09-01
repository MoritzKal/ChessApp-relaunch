package com.chessapp.api;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.http.MediaType;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;

import com.chessapp.api.domain.entity.Game;
import com.chessapp.api.domain.entity.GameResult;
import com.chessapp.api.domain.entity.Position;
import com.chessapp.api.domain.entity.Color;
import com.chessapp.api.domain.entity.Platform;
import com.chessapp.api.domain.entity.User;
import com.chessapp.api.domain.repo.GameRepository;
import com.chessapp.api.domain.repo.PositionRepository;
import com.chessapp.api.domain.repo.UserRepository;

@SpringBootTest(properties = {"logging.config=classpath:logback-spring.xml"}, classes = com.chessapp.api.codex.CodexApplication.class)
@org.springframework.test.context.ActiveProfiles("codex")
@AutoConfigureMockMvc
class GamesControllerTest extends com.chessapp.api.testutil.AbstractIntegrationTest {

    @Autowired
    MockMvc mockMvc;
    @Autowired
    UserRepository userRepository;
    @Autowired
    GameRepository gameRepository;
    @Autowired
    PositionRepository positionRepository;

    @Test
    void listDetailAndPositions() throws Exception {
        User u = new User();
        u.setId(UUID.randomUUID());
        u.setChessUsername("M3NG00S3");
        u.setCreatedAt(Instant.now());
        userRepository.save(u);

        Game g = new Game();
        g.setId(UUID.randomUUID());
        g.setUserId(u.getId());
        g.setEndTime(Instant.now());
        g.setTimeControl("5+0");
        g.setResult(GameResult.DRAW);
        g.setWhiteRating(1500);
        g.setBlackRating(1500);
        g.setPlatform(Platform.CHESS_COM);
        g.setPgn("pgn");
        gameRepository.save(g);

        Position p1 = new Position();
        p1.setId(UUID.randomUUID());
        p1.setGameId(g.getId());
        p1.setPly(1);
        p1.setFen("fen1");
        p1.setSideToMove("W");
        positionRepository.save(p1);
        Position p2 = new Position();
        p2.setId(UUID.randomUUID());
        p2.setGameId(g.getId());
        p2.setPly(2);
        p2.setFen("fen2");
        p2.setSideToMove("B");
        positionRepository.save(p2);

        mockMvc.perform(get("/v1/games").param("username", "M3NG00S3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(g.getId().toString()));

        mockMvc.perform(get("/v1/games/" + g.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pgnRaw").value("pgn"));

        mockMvc.perform(get("/v1/games/" + g.getId() + "/positions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].ply").value(1));
    }

    @Test
    void list_withoutUsername_returns400WithHelpfulMessage() throws Exception {
        mockMvc.perform(get("/v1/games").param("limit", "5"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(content().string(containsString("username")));
    }
}
