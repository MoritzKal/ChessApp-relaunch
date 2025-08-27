package com.chessapp.api.service;

import com.chessapp.api.domain.entity.Dataset;
import com.chessapp.api.service.dto.DatasetResponse;

public class DatasetMapper {
    public static DatasetResponse toDto(Dataset dataset) {
        return new DatasetResponse(
                dataset.getId(),
                dataset.getName(),
                dataset.getVersion(),
                dataset.getSizeRows(),
                dataset.getLocationUri(),
                dataset.getCreatedAt()
        );
    }
}
