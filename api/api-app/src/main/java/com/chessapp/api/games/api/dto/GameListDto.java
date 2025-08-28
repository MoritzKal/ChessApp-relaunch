package com.chessapp.api.games.api.dto;

import com.chessapp.api.domain.entity.Game;
import com.chessapp.api.domain.entity.GameResult;
import java.time.Instant;
import java.util.UUID;

public record GameListDto(
        UUID id,
        Instant endTime,
        String timeControl,
        GameResult result,
        Integer whiteRating,
        Integer blackRating
) {
    public static GameListDto fromEntity(Game g) {
        return new GameListDto(
            g.getId(),
            g.getEndTime(),
            g.getTimeControl(),
            g.getResult(),
            g.getWhiteRating(),
            g.getBlackRating()
        );
    }
}
