package com.chessapp.api.domain.repo;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.chessapp.api.domain.entity.User;

public interface UserRepository extends JpaRepository<User, UUID> {
    Optional<User> findByChessUsername(String chessUsername);
}
