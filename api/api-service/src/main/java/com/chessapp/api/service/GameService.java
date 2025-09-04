package com.chessapp.api.service;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.PageRequest;

import com.chessapp.api.domain.entity.Game;
import com.chessapp.api.domain.entity.GameResult;
import com.chessapp.api.domain.entity.Platform;
import com.chessapp.api.domain.entity.Position;
import com.chessapp.api.domain.entity.Color;
import com.chessapp.api.domain.repo.GameRepository;
import com.chessapp.api.domain.repo.PositionRepository;
import com.chessapp.api.domain.repo.UserRepository;
import com.chessapp.api.service.dto.GameDetailDto;
import com.chessapp.api.service.dto.GameSummaryDto;
import com.chessapp.api.service.dto.PositionDto;

@Service
public class GameService {

    private static final UUID DEFAULT_USER_ID = UUID.fromString("00000000-0000-0000-0000-000000000000");

    private final GameRepository gameRepository;
    private final PositionRepository positionRepository;
    private final UserRepository userRepository;

    public GameService(GameRepository gameRepository, PositionRepository positionRepository, UserRepository userRepository) {
        this.gameRepository = gameRepository;
        this.positionRepository = positionRepository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public List<GameSummaryDto> listGames(String username, int limit, int offset,
                                          GameResult result, Color color, LocalDate since) {
        UUID userId = userRepository.findByChessUsername(username)
                .map(u -> u.getId())
                .orElse(DEFAULT_USER_ID);
        return gameRepository.findRecentByUser(userId, limit, offset,
                        result != null ? result.name() : null,
                        color != null ? color.name() : null,
                        since)
                .stream()
                .map(GameMapper::toSummary)
                .toList();
    }

    @Transactional(readOnly = true)
    public GameDetailDto getGame(UUID id) {
        Game game = gameRepository.findById(id).orElseThrow();
        return GameMapper.toDetail(game);
    }

    @Transactional(readOnly = true)
    public List<PositionDto> listPositions(UUID gameId) {
        List<Position> positions = positionRepository.findByGameIdOrderByPlyAsc(gameId);
        return positions.stream().map(GameMapper::toPositionDto).toList();
    }

    @Transactional(readOnly = true)
    public List<GameSummaryDto> listRecent(int limit) {
        return gameRepository.findAllByOrderByEndTimeDesc(PageRequest.of(0, limit))
                .getContent()
                .stream()
                .map(GameMapper::toSummary)
                .toList();
    }

    @Transactional
    public Game createMinimalGame() {
        Game g = new Game();
        g.setId(UUID.randomUUID());
        g.setUserId(UUID.randomUUID());
        g.setPlatform(Platform.CHESS_COM);
        g.setEndTime(Instant.now());
        return gameRepository.save(g);
    }
}
