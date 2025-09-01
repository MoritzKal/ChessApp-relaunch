package com.chessapp.api.domain.entity;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import com.chessapp.api.common.validation.ValidPgn;

import com.vladmihalcea.hibernate.type.json.JsonType;
import org.hibernate.annotations.Type;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import com.chessapp.api.domain.entity.Platform;
import com.chessapp.api.domain.entity.GameResult;
import com.chessapp.api.domain.entity.TimeControlCategory;

@Entity
@Table(name = "games")
public class Game {

    @Id
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "game_id_ext")
    private String gameIdExt;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "platform", columnDefinition = "platform")
    private Platform platform;

    @Column(name = "end_time")
    private Instant endTime;

    @Column(name = "time_control")
    private String timeControl;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "time_category", columnDefinition = "time_category")
    private TimeControlCategory timeCategory;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "result", columnDefinition = "game_result")
    private GameResult result;

    @Column(name = "white_rating")
    private Integer whiteRating;

    @Column(name = "black_rating")
    private Integer blackRating;

    @Column(name = "pgn", columnDefinition = "text", nullable = false)
    @ValidPgn
    private String pgn;

    @Type(JsonType.class)
    @Column(columnDefinition = "jsonb")
    private Map<String, Object> tags;

    // getters and setters
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getUserId() { return userId; }
    public void setUserId(UUID userId) { this.userId = userId; }
    public String getGameIdExt() { return gameIdExt; }
    public void setGameIdExt(String gameIdExt) { this.gameIdExt = gameIdExt; }
    public Platform getPlatform() { return platform; }
    public void setPlatform(Platform platform) { this.platform = platform; }
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
    public String getPgn() { return pgn; }
    public void setPgn(String pgn) { this.pgn = pgn; }
    public Map<String, Object> getTags() { return tags; }
    public void setTags(Map<String, Object> tags) { this.tags = tags; }
}
