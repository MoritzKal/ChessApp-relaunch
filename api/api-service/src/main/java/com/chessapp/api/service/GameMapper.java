package com.chessapp.api.service;

import com.chessapp.api.domain.entity.Game;
import com.chessapp.api.domain.entity.Position;
import com.chessapp.api.domain.entity.Color;
import com.chessapp.api.service.dto.GameDetailDto;
import com.chessapp.api.service.dto.GameSummaryDto;
import com.chessapp.api.service.dto.PositionDto;

public class GameMapper {
    public static GameSummaryDto toSummary(Game game) {
        return new GameSummaryDto(
                game.getId(),
                game.getEndTime(),
                game.getTimeControl(),
                game.getResult(),
                game.getWhiteRating(),
                game.getBlackRating()
        );
    }

    public static GameDetailDto toDetail(Game game) {
        return new GameDetailDto(
                game.getId(),
                game.getEndTime(),
                game.getTimeControl(),
                game.getResult(),
                game.getWhiteRating(),
                game.getBlackRating(),
                game.getPgn()
        );
    }

    public static PositionDto toPositionDto(Position position) {
        Color side = "W".equalsIgnoreCase(position.getSideToMove()) ? Color.WHITE : Color.BLACK;
        return new PositionDto(
                position.getPly(),
                position.getFen(),
                side
        );
    }
}
