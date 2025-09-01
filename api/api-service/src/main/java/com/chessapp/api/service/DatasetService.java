package com.chessapp.api.service;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.chessapp.api.domain.entity.Dataset;
import com.chessapp.api.domain.repo.DatasetRepository;
import com.chessapp.api.service.dto.DatasetCreateRequest;
import com.chessapp.api.service.dto.DatasetResponse;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.micrometer.core.instrument.Counter;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

@Service
public class DatasetService {

    private static final Logger log = LoggerFactory.getLogger(DatasetService.class);

    private final DatasetRepository datasetRepository;
    private final S3Client s3Client;
    private final ObjectMapper objectMapper;
    private final Counter datasetBuildCounter;
    private final AtomicLong datasetRowsGauge;

    public DatasetService(DatasetRepository datasetRepository,
                          S3Client s3Client,
                          ObjectMapper objectMapper,
                          Counter datasetBuildCounter,
                          AtomicLong datasetRowsGauge) {
        this.datasetRepository = datasetRepository;
        this.s3Client = s3Client;
        this.objectMapper = objectMapper;
        this.datasetBuildCounter = datasetBuildCounter;
        this.datasetRowsGauge = datasetRowsGauge;
    }

    @Transactional
    public DatasetResponse create(DatasetCreateRequest req) {
        UUID id = UUID.randomUUID();
        final String key = id + "/manifest.json";
        final String locationUri = "s3://datasets/" + key;
        Dataset d = new Dataset();
        d.setId(id);
        d.setName(req.getName());
        d.setVersion(req.getVersion());
        d.setFilter(req.getFilter() != null ? req.getFilter() : Map.of());
        d.setSplit(req.getSplit() != null ? req.getSplit() : Map.of());
        long rows = req.getSizeRows() != null ? req.getSizeRows() : 0L;
        d.setSizeRows(rows);
        d.setCreatedAt(Instant.now());
        d.setLocationUri(locationUri);
        datasetRepository.save(d);

        Map<String, Object> manifest = new LinkedHashMap<>();
        manifest.put("id", id.toString());
        manifest.put("name", d.getName());
        manifest.put("version", d.getVersion());
        manifest.put("filter", d.getFilter());
        manifest.put("split", d.getSplit());
        manifest.put("createdAt", d.getCreatedAt());
        manifest.put("username", MDC.get("username"));

        try {
            String json = objectMapper.writeValueAsString(manifest);
            PutObjectRequest request = PutObjectRequest.builder()
                    .bucket("datasets")
                    .key(key)
                    .contentType("application/json")
                    .build();
            s3Client.putObject(request, RequestBody.fromString(json));
        } catch (Exception e) {
            throw new RuntimeException("Failed to upload manifest", e);
        }

        datasetBuildCounter.increment();
        if (req.getSizeRows() != null) {
            datasetRowsGauge.set(req.getSizeRows());
        }

        try (MDC.MDCCloseable c1 = MDC.putCloseable("dataset_id", id.toString());
             MDC.MDCCloseable c2 = MDC.putCloseable("event", "dataset.created")) {
            log.info("dataset created");
        }

        return DatasetMapper.toDto(d);
    }

    @Transactional(readOnly = true)
    public List<DatasetResponse> list(int limit, int offset) {
        return datasetRepository.findAll(PageRequest.of(offset / limit, limit,
                        Sort.by(Sort.Direction.DESC, "createdAt")))
                .stream()
                .map(DatasetMapper::toDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public DatasetResponse get(UUID id) {
        return datasetRepository.findById(id)
                .map(DatasetMapper::toDto)
                .orElseThrow();
    }
}
