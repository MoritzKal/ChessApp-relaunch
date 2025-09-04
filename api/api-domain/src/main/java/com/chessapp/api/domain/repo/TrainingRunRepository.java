package com.chessapp.api.domain.repo;

import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.chessapp.api.domain.entity.TrainingRun;
import com.chessapp.api.domain.entity.TrainingStatus;

public interface TrainingRunRepository extends JpaRepository<TrainingRun, UUID> {
    long countByStatus(TrainingStatus status);
    List<TrainingRun> findAllByStatusOrderByStartedAtDesc(TrainingStatus status, Pageable pageable);
    List<TrainingRun> findAllByOrderByStartedAtDesc(Pageable pageable);
}
