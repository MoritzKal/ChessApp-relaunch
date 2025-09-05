package com.chessapp.api.domain.repo;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.chessapp.api.domain.entity.Dataset;

public interface DatasetRepository extends JpaRepository<Dataset, UUID> {
    Optional<Dataset> findByName(String name);
    Optional<Dataset> findByNameIgnoreCase(String name);
    boolean existsByNameIgnoreCase(String name);
}
