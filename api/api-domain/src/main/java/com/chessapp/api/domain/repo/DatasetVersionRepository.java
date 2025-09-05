package com.chessapp.api.domain.repo;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.chessapp.api.domain.entity.DatasetVersion;

public interface DatasetVersionRepository extends JpaRepository<DatasetVersion, UUID> {
    Optional<DatasetVersion> findByDatasetIdAndVersion(UUID datasetId, String version);
}
