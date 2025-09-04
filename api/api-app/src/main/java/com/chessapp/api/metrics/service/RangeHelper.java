package com.chessapp.api.metrics.service;

import java.time.Duration;
import java.time.Instant;

public final class RangeHelper {
    private RangeHelper() {}

    public static RangeParams mapRange(String range) {
        Instant end = Instant.now();
        Instant start;
        String step;
        switch (range) {
            case "2h" -> { start = end.minus(Duration.ofHours(2)); step = "60s"; }
            case "24h" -> { start = end.minus(Duration.ofHours(24)); step = "300s"; }
            case "7d" -> { start = end.minus(Duration.ofDays(7)); step = "1800s"; }
            case "30d" -> { start = end.minus(Duration.ofDays(30)); step = "7200s"; }
            default -> { start = end.minus(Duration.ofHours(2)); step = "60s"; }
        }
        return new RangeParams(start.getEpochSecond(), end.getEpochSecond(), step);
    }
}
