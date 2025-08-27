package com.chessapp.api.domain.repo;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.chessapp.api.domain.entity.Position;

public interface PositionRepository extends JpaRepository<Position, UUID> {
    List<Position> findByGameIdOrderByPlyAsc(UUID gameId);
}
