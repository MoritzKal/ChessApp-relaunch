package com.chessapp.api.datasets.service;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.chessapp.api.domain.entity.Dataset;
import com.chessapp.api.domain.entity.DatasetVersion;
import com.chessapp.api.domain.repo.DatasetRepository;
import com.chessapp.api.domain.repo.DatasetVersionRepository;

@Service
public class DatasetCatalogService {

    private static final Logger log = LoggerFactory.getLogger(DatasetCatalogService.class);

    private final DatasetRepository datasetRepository;
    private final DatasetVersionRepository versionRepository;

    public DatasetCatalogService(DatasetRepository datasetRepository,
                                 DatasetVersionRepository versionRepository) {
        this.datasetRepository = datasetRepository;
        this.versionRepository = versionRepository;
    }

    @Transactional
    public void registerIfAbsent(String datasetName, @Nullable String displayName) {
        var existing = datasetRepository.findByNameIgnoreCase(datasetName);
        if (existing.isPresent()) {
            log.info("dataset registered name={} id={} upserted=false", datasetName, existing.get().getId());
            return;
        }
        Dataset d = new Dataset();
        d.setId(UUID.randomUUID());
        d.setName(datasetName);
        d.setVersion("v0");
        d.setSizeRows(0L);
        d.setSizeBytes(0L);
        d.setFilter(Map.of());
        d.setSplit(Map.of());
        d.setCreatedAt(Instant.now());
        d.setUpdatedAt(Instant.now());
        datasetRepository.save(d);
        log.info("dataset registered name={} id={} upserted=true", datasetName, d.getId());
    }

    @Transactional
    public void addVersion(String datasetName, String version, long rows, long sizeBytes) {
        Dataset d = datasetRepository.findByNameIgnoreCase(datasetName)
                .orElseThrow();
        boolean created = false;
        DatasetVersion dv = versionRepository.findByDatasetIdAndVersion(d.getId(), version)
                .orElse(null);
        if (dv == null) {
            dv = new DatasetVersion();
            dv.setDatasetId(d.getId());
            dv.setVersion(version);
            created = true;
        }
        dv.setRows(rows);
        dv.setSizeBytes(sizeBytes);
        versionRepository.save(dv);

        d.setVersion(version);
        d.setSizeRows(rows);
        d.setSizeBytes(sizeBytes);
        d.setUpdatedAt(Instant.now());
        datasetRepository.save(d);

        log.info("dataset version cataloged dataset={} version={} upserted={}", datasetName, version, created);
    }
}
