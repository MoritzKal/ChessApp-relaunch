package com.chessapp.api.ingest.repo;

import com.chessapp.api.ingest.entity.IngestRunEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface IngestRunRepository extends JpaRepository<IngestRunEntity, UUID> {
}
