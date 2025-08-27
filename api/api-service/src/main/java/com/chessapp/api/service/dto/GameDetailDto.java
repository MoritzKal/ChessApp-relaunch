package com.chessapp.api.service.dto;

import java.time.Instant;
import java.util.UUID;

import com.chessapp.api.domain.entity.GameResult;

/**
 * Detailed game information including raw PGN.
 */
public class GameDetailDto {
    private UUID id;
    private Instant endTime;
    private String timeControl;
    private GameResult result;
    private Integer whiteRating;
    private Integer blackRating;
    private String pgnRaw;

    public GameDetailDto() {
    }

    public GameDetailDto(UUID id, Instant endTime, String timeControl, GameResult result,
                         Integer whiteRating, Integer blackRating, String pgnRaw) {
        this.id = id;
        this.endTime = endTime;
        this.timeControl = timeControl;
        this.result = result;
        this.whiteRating = whiteRating;
        this.blackRating = blackRating;
        this.pgnRaw = pgnRaw;
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

    public String getPgnRaw() {
        return pgnRaw;
    }

    public void setPgnRaw(String pgnRaw) {
        this.pgnRaw = pgnRaw;
    }
}
