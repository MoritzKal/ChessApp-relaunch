package com.chessapp.api.domain.entity;

import java.util.List;
import java.util.UUID;
import java.util.ArrayList;

import org.hibernate.annotations.JdbcTypeCode;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "positions")
public class Position {
    @Id
    private UUID id;

    @Column(name = "game_id", nullable = false)
    private UUID gameId;

    @Column(nullable = false)
    private int ply;

    @Column(nullable = false)
    private String fen;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "side_to_move", columnDefinition = "char(1)")
    private String sideToMove;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "legal_moves", columnDefinition = "jsonb", nullable = false)
    private List<String> legalMoves = new ArrayList<>();

    @PrePersist
    void prePersist() {
        if (legalMoves == null) legalMoves = new ArrayList<>();
    }

    // getters and setters
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getGameId() { return gameId; }
    public void setGameId(UUID gameId) { this.gameId = gameId; }
    public int getPly() { return ply; }
    public void setPly(int ply) { this.ply = ply; }
    public String getFen() { return fen; }
    public void setFen(String fen) { this.fen = fen; }
    public String getSideToMove() { return sideToMove; }
    public void setSideToMove(String sideToMove) { this.sideToMove = sideToMove; }
    public List<String> getLegalMoves() { return legalMoves; }
    public void setLegalMoves(List<String> legalMoves) { this.legalMoves = legalMoves; }
}
