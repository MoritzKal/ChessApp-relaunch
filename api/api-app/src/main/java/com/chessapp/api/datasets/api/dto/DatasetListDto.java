package com.chessapp.api.datasets.api.dto;

import java.util.List;

public record DatasetListDto(List<DatasetListItemDto> items, Integer nextOffset) {}
