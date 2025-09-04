package com.chessapp.api.datasets.api.dto;

public record ColumnDto(String name, String dtype, double nullPct, double uniquePct, String min, String max) {}
