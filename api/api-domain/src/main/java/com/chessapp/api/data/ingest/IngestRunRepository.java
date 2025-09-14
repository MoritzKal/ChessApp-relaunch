package com.chessapp.api.data.ingest;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

/**
 * Repository for {@link IngestRunEntity}.
 */
public interface IngestRunRepository extends JpaRepository<IngestRunEntity, UUID> {
    java.util.List<IngestRunEntity> findAllByDatasetIdOrderByStartedAtDesc(String datasetId);
}
