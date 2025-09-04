package com.chessapp.api.datasets.api.dto;

import java.util.List;

public record VersionsDto(int count, String latest, List<VersionItemDto> items) {}
