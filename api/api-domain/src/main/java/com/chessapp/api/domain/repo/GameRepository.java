package com.chessapp.api.domain.repo;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.lang.Nullable;

import com.chessapp.api.domain.entity.Game;
import com.chessapp.api.domain.entity.Platform;

public interface GameRepository extends JpaRepository<Game, UUID> {

    Optional<Game> findByGameIdExt(String gameIdExt);
    Page<Game> findAllByOrderByEndTimeDesc(Pageable pageable);
    Page<Game> findByUserIdOrderByEndTimeDesc(UUID userId, Pageable pageable);

    Optional<Game> findByPlatformAndGameIdExt(Platform platform, String gameIdExt);

    @Query(value = """
            SELECT * FROM games
            WHERE user_id = :userId
              AND (:result IS NULL OR result = :result)
              AND (:color IS NULL OR tags->>'color' = :color)
              AND (:since IS NULL OR end_time >= :since)
            ORDER BY end_time DESC
            LIMIT :limit OFFSET :offset
            """, nativeQuery = true)
    List<Game> findRecentByUser(
            @Param("userId") UUID userId,
            @Param("limit") int limit,
            @Param("offset") int offset,
            @Param("result") @Nullable String result,
            @Param("color") @Nullable String color,
            @Param("since") @Nullable LocalDate since);
}
