package com.chessapp.api.service;

import com.chessapp.api.domain.entity.Game;
import com.chessapp.api.service.dto.GameListItemDto;

public class GameMapper {
    public static GameListItemDto toDto(Game game) {
        return new GameListItemDto(
                game.getId(),
                game.getEndTime(),
                game.getTimeControl(),
                game.getTimeCategory(),
                game.getResult(),
                game.getWhiteRating(),
                game.getBlackRating()
        );
    }
}
