package com.chessapp.api.domain.entity;

import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;

@Entity
@Table(name = "moves")
public class Move {
    @Id
    private UUID id;

    @Column(name = "game_id", nullable = false)
    private UUID gameId;

    @Column(nullable = false)
    private int ply;

    private String san;
    private String uci;

    @Enumerated(EnumType.STRING)
    private Color color;

    @Column(name = "clock_ms")
    private Integer clockMs;

    @Column(name = "eval_cp")
    private Integer evalCp;

    @Column(name = "is_blunder")
    private Boolean isBlunder;

    @Lob
    private String comment;

    // getters and setters
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getGameId() { return gameId; }
    public void setGameId(UUID gameId) { this.gameId = gameId; }
    public int getPly() { return ply; }
    public void setPly(int ply) { this.ply = ply; }
    public String getSan() { return san; }
    public void setSan(String san) { this.san = san; }
    public String getUci() { return uci; }
    public void setUci(String uci) { this.uci = uci; }
    public Color getColor() { return color; }
    public void setColor(Color color) { this.color = color; }
    public Integer getClockMs() { return clockMs; }
    public void setClockMs(Integer clockMs) { this.clockMs = clockMs; }
    public Integer getEvalCp() { return evalCp; }
    public void setEvalCp(Integer evalCp) { this.evalCp = evalCp; }
    public Boolean getIsBlunder() { return isBlunder; }
    public void setIsBlunder(Boolean isBlunder) { this.isBlunder = isBlunder; }
    public String getComment() { return comment; }
    public void setComment(String comment) { this.comment = comment; }
}
