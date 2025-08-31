package com.chessapp.api.dataset;

import com.chessapp.api.dataset.dto.DatasetResponse;

public class DatasetMapper {
    public static DatasetResponse toDto(DatasetEntity dataset) {
        return new DatasetResponse(
                dataset.getId(),
                dataset.getName(),
                dataset.getVersion(),
                dataset.getFilterJson(),
                dataset.getSplitJson(),
                dataset.getSizeRows(),
                dataset.getLocationUri(),
                dataset.getCreatedAt()
        );
    }
}
