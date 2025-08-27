package com.chessapp.api.service.dto;

import java.time.Instant;
import java.util.UUID;

import com.chessapp.api.domain.entity.GameResult;
import com.chessapp.api.domain.entity.TimeControlCategory;

public class GameListItemDto {
    private UUID id;
    private Instant endTime;
    private String timeControl;
    private TimeControlCategory timeCategory;
    private GameResult result;
    private Integer whiteRating;
    private Integer blackRating;

    public GameListItemDto() {
    }

    public GameListItemDto(UUID id, Instant endTime, String timeControl, TimeControlCategory timeCategory,
                           GameResult result, Integer whiteRating, Integer blackRating) {
        this.id = id;
        this.endTime = endTime;
        this.timeControl = timeControl;
        this.timeCategory = timeCategory;
        this.result = result;
        this.whiteRating = whiteRating;
        this.blackRating = blackRating;
    }

    // getters and setters
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public Instant getEndTime() { return endTime; }
    public void setEndTime(Instant endTime) { this.endTime = endTime; }
    public String getTimeControl() { return timeControl; }
    public void setTimeControl(String timeControl) { this.timeControl = timeControl; }
    public TimeControlCategory getTimeCategory() { return timeCategory; }
    public void setTimeCategory(TimeControlCategory timeCategory) { this.timeCategory = timeCategory; }
    public GameResult getResult() { return result; }
    public void setResult(GameResult result) { this.result = result; }
    public Integer getWhiteRating() { return whiteRating; }
    public void setWhiteRating(Integer whiteRating) { this.whiteRating = whiteRating; }
    public Integer getBlackRating() { return blackRating; }
    public void setBlackRating(Integer blackRating) { this.blackRating = blackRating; }
}
