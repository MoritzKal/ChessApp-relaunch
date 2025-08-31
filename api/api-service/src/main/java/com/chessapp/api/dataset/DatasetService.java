package com.chessapp.api.dataset;

import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.chessapp.api.dataset.dto.CreateDatasetRequest;
import com.chessapp.api.dataset.dto.DatasetResponse;

@Service
public class DatasetService {

    private static final Logger log = LoggerFactory.getLogger(DatasetService.class);

    private final DatasetRepository datasetRepository;

    public DatasetService(DatasetRepository datasetRepository) {
        this.datasetRepository = datasetRepository;
    }

    @Transactional
    public DatasetResponse create(CreateDatasetRequest req) {
        DatasetEntity d = new DatasetEntity();
        d.setName(req.getName());
        d.setVersion(req.getVersion());
        d.setFilter(req.getFilterJson());
        d.setSplit(req.getSplitJson());
        d.setSizeRows(req.getSizeRows());
        d.setLocationUri(req.getLocationUri());
        datasetRepository.save(d);

        try (MDC.MDCCloseable c1 = MDC.putCloseable("dataset_id", d.getId().toString());
             MDC.MDCCloseable c2 = MDC.putCloseable("event", "dataset.created")) {
            log.info("dataset created");
        }
        return DatasetMapper.toDto(d);
    }

    @Transactional(readOnly = true)
    public List<DatasetResponse> list(int limit, int offset) {
        log.info("dataset list");
        return datasetRepository.findAll(PageRequest.of(offset / limit, limit))
                .stream()
                .map(DatasetMapper::toDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public DatasetResponse get(UUID id) {
        try (MDC.MDCCloseable c1 = MDC.putCloseable("dataset_id", id.toString());
             MDC.MDCCloseable c2 = MDC.putCloseable("event", "dataset.read")) {
            log.info("dataset read");
            return datasetRepository.findById(id)
                    .map(DatasetMapper::toDto)
                    .orElseThrow();
        }
    }
}
