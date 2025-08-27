package com.chessapp.api.domain.entity;

import java.util.List;
import java.util.UUID;

import org.hibernate.annotations.Type;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

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

    @Enumerated(EnumType.STRING)
    @Column(name = "side_to_move")
    private Color sideToMove;

    @Type(org.hibernate.type.JsonType.class)
    @Column(name = "legal_moves", columnDefinition = "jsonb")
    private List<String> legalMoves;

    // getters and setters
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getGameId() { return gameId; }
    public void setGameId(UUID gameId) { this.gameId = gameId; }
    public int getPly() { return ply; }
    public void setPly(int ply) { this.ply = ply; }
    public String getFen() { return fen; }
    public void setFen(String fen) { this.fen = fen; }
    public Color getSideToMove() { return sideToMove; }
    public void setSideToMove(Color sideToMove) { this.sideToMove = sideToMove; }
    public List<String> getLegalMoves() { return legalMoves; }
    public void setLegalMoves(List<String> legalMoves) { this.legalMoves = legalMoves; }
}
