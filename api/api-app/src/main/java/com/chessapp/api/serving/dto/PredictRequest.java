package com.chessapp.api.serving.dto;

import com.chessapp.api.common.validation.ValidFen;

public record PredictRequest(@ValidFen String fen) {}
