package com.chessapp.api.ingest.repo;

import com.chessapp.api.ingest.entity.IngestRun;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface IngestRunRepository extends JpaRepository<IngestRun, UUID> {
}
