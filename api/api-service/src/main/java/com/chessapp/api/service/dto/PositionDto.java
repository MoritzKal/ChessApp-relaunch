package com.chessapp.api.service.dto;

import com.chessapp.api.domain.entity.Color;

/**
 * Position information for a game.
 */
public class PositionDto {
    private int ply;
    private String fen;
    private Color sideToMove;

    public PositionDto() {
    }

    public PositionDto(int ply, String fen, Color sideToMove) {
        this.ply = ply;
        this.fen = fen;
        this.sideToMove = sideToMove;
    }

    public int getPly() {
        return ply;
    }

    public void setPly(int ply) {
        this.ply = ply;
    }

    public String getFen() {
        return fen;
    }

    public void setFen(String fen) {
        this.fen = fen;
    }

    public Color getSideToMove() {
        return sideToMove;
    }

    public void setSideToMove(Color sideToMove) {
        this.sideToMove = sideToMove;
    }
}
