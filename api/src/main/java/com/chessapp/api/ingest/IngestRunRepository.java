package com.chessapp.api.ingest;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface IngestRunRepository extends JpaRepository<IngestRunEntity, UUID> {}

