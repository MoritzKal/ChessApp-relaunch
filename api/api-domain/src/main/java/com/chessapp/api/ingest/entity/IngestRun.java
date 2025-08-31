package com.chessapp.api.ingest.entity;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

@Entity
@Table(name = "ingest_runs")
public class IngestRun {

    @Id
    private UUID id;

    @Column(nullable = false)
    private String username;

    @Column(name = "from_month", nullable = false)
    private String fromMonth;

    @Column(name = "to_month", nullable = false)
    private String toMonth;

    @Column(nullable = false)
    private String status;

    @Column(name = "started_at", nullable = false)
    private Instant startedAt;

    @Column(name = "finished_at")
    private Instant finishedAt;

    @Column(name = "games_count")
    private Integer gamesCount = 0;

    @Column(name = "moves_count")
    private Long movesCount = 0L;

    @Column(name = "positions_count")
    private Long positionsCount = 0L;

    private String error;

    @Column(name = "report_uri")
    private String reportUri;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    public void prePersist() {
        if (id == null) {
            id = UUID.randomUUID();
        }
        createdAt = Instant.now();
        updatedAt = createdAt;
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = Instant.now();
    }

    // getters and setters
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getFromMonth() { return fromMonth; }
    public void setFromMonth(String fromMonth) { this.fromMonth = fromMonth; }
    public String getToMonth() { return toMonth; }
    public void setToMonth(String toMonth) { this.toMonth = toMonth; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Instant getStartedAt() { return startedAt; }
    public void setStartedAt(Instant startedAt) { this.startedAt = startedAt; }
    public Instant getFinishedAt() { return finishedAt; }
    public void setFinishedAt(Instant finishedAt) { this.finishedAt = finishedAt; }
    public Integer getGamesCount() { return gamesCount; }
    public void setGamesCount(Integer gamesCount) { this.gamesCount = gamesCount; }
    public Long getMovesCount() { return movesCount; }
    public void setMovesCount(Long movesCount) { this.movesCount = movesCount; }
    public Long getPositionsCount() { return positionsCount; }
    public void setPositionsCount(Long positionsCount) { this.positionsCount = positionsCount; }
    public String getError() { return error; }
    public void setError(String error) { this.error = error; }
    public String getReportUri() { return reportUri; }
    public void setReportUri(String reportUri) { this.reportUri = reportUri; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
