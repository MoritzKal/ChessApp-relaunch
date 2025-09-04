package com.chessapp.api.account.dto;

public record UserPrefsDto(double temperature, int topk, String boardOrientation, boolean useGameAsTraining) {}
