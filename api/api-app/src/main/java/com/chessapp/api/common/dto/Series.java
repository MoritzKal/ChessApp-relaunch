package com.chessapp.api.common.dto;

import java.util.List;

public record Series(String label, List<TimeSeriesPoint> points) {}
