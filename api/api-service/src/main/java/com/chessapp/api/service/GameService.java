package com.chessapp.api.service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.chessapp.api.domain.entity.Game;
import com.chessapp.api.domain.entity.Platform;
import com.chessapp.api.domain.repo.GameRepository;
import com.chessapp.api.service.dto.GameListItemDto;

@Service
public class GameService {

    private final GameRepository gameRepository;

    public GameService(GameRepository gameRepository) {
        this.gameRepository = gameRepository;
    }

    @Transactional(readOnly = true)
    public List<GameListItemDto> listGames(int limit, int offset) {
        return gameRepository.findAll(PageRequest.of(offset / limit, limit))
                .stream()
                .map(GameMapper::toDto)
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
