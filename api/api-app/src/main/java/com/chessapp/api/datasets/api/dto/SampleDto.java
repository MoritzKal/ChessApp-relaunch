package com.chessapp.api.datasets.api.dto;

import java.util.List;
import java.util.Map;

public record SampleDto(List<Map<String, Object>> items, String nextCursor) {}
