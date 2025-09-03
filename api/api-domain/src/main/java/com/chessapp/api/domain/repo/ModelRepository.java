package com.chessapp.api.domain.repo;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.chessapp.api.domain.entity.Model;

public interface ModelRepository extends JpaRepository<Model, UUID> {
    Optional<Model> findByIsProdTrue();

    @Modifying
    @Query("UPDATE Model m SET m.isProd = false WHERE m.isProd = true")
    int clearProd();

    @Modifying
    @Query("UPDATE Model m SET m.isProd = true WHERE m.id = :id")
    int markProd(@Param("id") UUID id);
}
