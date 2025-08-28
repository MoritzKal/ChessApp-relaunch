package com.chessapp.api.domain.entity;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.*;

@Entity
@Table(name = "users")
public class User {

    @Id
    private UUID id;

    @Column(name = "chess_username", nullable = false)
    private String chessUsername;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getChessUsername() {
        return chessUsername;
    }

    public void setChessUsername(String chessUsername) {
        this.chessUsername = chessUsername;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    @PrePersist
    public void prePersist() {
        if (createdAt == null) {
            createdAt = java.time.Instant.now();
        }
    }
}
