package com.chessapp.api.domain.repo;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.chessapp.api.domain.entity.Game;

public interface GameRepository extends JpaRepository<Game, UUID> {
}
