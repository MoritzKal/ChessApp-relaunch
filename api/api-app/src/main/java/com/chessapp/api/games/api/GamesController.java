package com.chessapp.api.games.api;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.chessapp.api.domain.entity.Color;
import com.chessapp.api.domain.entity.GameResult;
import com.chessapp.api.service.GameService;
import com.chessapp.api.service.dto.GameDetailDto;
import com.chessapp.api.service.dto.GameSummaryDto;
import com.chessapp.api.service.dto.PositionDto;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/v1/games")
@Tag(name = "games")
public class GamesController {

    private final GameService gameService;

    public GamesController(GameService gameService) {
        this.gameService = gameService;
    }

    @GetMapping
    @Operation(summary = "List games for a user")
    public java.util.List<GameSummaryDto> list(
            @Parameter(description = "Username to fetch games for", required = true) @RequestParam(required = false) String username,
            @RequestParam(defaultValue = "50") int limit,
            @RequestParam(defaultValue = "0") int offset,
            @RequestParam(required = false) GameResult result,
            @RequestParam(required = false) Color color,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate since) {
        if (username == null || username.isBlank()) {
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.BAD_REQUEST,
                    "username is required"
            );
        }
        return gameService.listGames(username, limit, offset, result, color, since);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get game detail by id")
    public GameDetailDto get(@PathVariable UUID id) {
        return gameService.getGame(id);
    }

    @GetMapping("/{id}/positions")
    @Operation(summary = "List positions for a game")
    public List<PositionDto> positions(@PathVariable UUID id) {
        return gameService.listPositions(id);
    }
}
