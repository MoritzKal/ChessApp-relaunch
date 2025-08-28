package com.chessapp.api.service.dto;

import java.time.Instant;
import java.util.UUID;

import com.chessapp.api.domain.entity.GameResult;

/**
 * Summary information for a game returned in list endpoints.
 */
public class GameSummaryDto {
    private UUID id;
    private Instant endTime;
    private String timeControl;
    private GameResult result;
    private Integer whiteRating;
    private Integer blackRating;

    public GameSummaryDto() {
    }

    public GameSummaryDto(UUID id, Instant endTime, String timeControl, GameResult result,
                          Integer whiteRating, Integer blackRating) {
        this.id = id;
        this.endTime = endTime;
        this.timeControl = timeControl;
        this.result = result;
        this.whiteRating = whiteRating;
        this.blackRating = blackRating;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public Instant getEndTime() {
        return endTime;
    }

    public void setEndTime(Instant endTime) {
        this.endTime = endTime;
    }

    public String getTimeControl() {
        return timeControl;
    }

    public void setTimeControl(String timeControl) {
        this.timeControl = timeControl;
    }

    public GameResult getResult() {
        return result;
    }

    public void setResult(GameResult result) {
        this.result = result;
    }

    public Integer getWhiteRating() {
        return whiteRating;
    }

    public void setWhiteRating(Integer whiteRating) {
        this.whiteRating = whiteRating;
    }

    public Integer getBlackRating() {
        return blackRating;
    }

    public void setBlackRating(Integer blackRating) {
        this.blackRating = blackRating;
    }
}
