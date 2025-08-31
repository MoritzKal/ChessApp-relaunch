package com.chessapp.api.dataset;

import java.util.UUID;

import jakarta.persistence.EntityNotFoundException;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.chessapp.api.dataset.dto.CreateDatasetRequest;
import com.chessapp.api.dataset.dto.DatasetResponse;

@Service
@Transactional
public class DatasetService {

    private final DatasetRepository datasetRepository;

    public DatasetService(DatasetRepository datasetRepository) {
        this.datasetRepository = datasetRepository;
    }

    /**
     * Persist a new dataset and return its identifier.
     */
    public UUID create(CreateDatasetRequest req) {
        DatasetEntity entity = new DatasetEntity();
        entity.setName(req.getName());
        entity.setVersion(req.getVersion());
        entity.setFilter(req.getFilterJson());
        entity.setSplit(req.getSplitJson());
        entity.setSizeRows(req.getSizeRows());
        entity.setLocationUri(req.getLocationUri());
        datasetRepository.save(entity);
        return entity.getId();
    }

    /**
     * Retrieve a dataset by id.
     */
    @Transactional(readOnly = true)
    public DatasetResponse get(UUID id) {
        DatasetEntity entity = datasetRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("dataset not found: " + id));
        return DatasetMapper.toDto(entity);
    }

    /**
     * List datasets with pagination.
     */
    @Transactional(readOnly = true)
    public Page<DatasetResponse> list(Pageable pageable) {
        return datasetRepository.findAll(pageable).map(DatasetMapper::toDto);
    }
}
