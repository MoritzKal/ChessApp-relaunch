package com.chessapp.api.web;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.chessapp.api.domain.entity.Game;
import com.chessapp.api.service.GameService;
import com.chessapp.api.service.dto.GameListItemDto;

@RestController
@RequestMapping("/v1/games")
public class GameController {

    private final GameService gameService;

    public GameController(GameService gameService) {
        this.gameService = gameService;
    }

    @GetMapping
    public List<GameListItemDto> list(@RequestParam(defaultValue = "50") int limit,
                                      @RequestParam(defaultValue = "0") int offset) {
        return gameService.listGames(limit, offset);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Game create() {
        return gameService.createMinimalGame();
    }
}
