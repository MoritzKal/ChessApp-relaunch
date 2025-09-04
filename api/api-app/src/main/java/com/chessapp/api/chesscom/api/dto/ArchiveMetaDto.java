package com.chessapp.api.chesscom.api.dto;

import java.util.Map;

public record ArchiveMetaDto(int count, Map<String, Integer> timeControlDist) {}
