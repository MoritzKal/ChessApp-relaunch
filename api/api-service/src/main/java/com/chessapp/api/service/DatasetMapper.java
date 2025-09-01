package com.chessapp.api.service;

import com.chessapp.api.domain.entity.DatasetEntity;
import com.chessapp.api.service.dto.DatasetResponse;

public class DatasetMapper {
    public static DatasetResponse toDto(DatasetEntity dataset) {
        return new DatasetResponse(
                dataset.getId(),
                dataset.getName(),
                dataset.getVersion(),
                dataset.getFilter(),
                dataset.getSplit(),
                dataset.getSizeRows(),
                dataset.getLocationUri(),
                dataset.getCreatedAt()
        );
    }
}
