package com.chessapp.api.ingest.entity;

import com.chessapp.api.ingest.IngestRunStatus;
import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "ingest_runs")
public class IngestRunEntity {

    @Id
    @Column(name = "run_id")
    private UUID runId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private IngestRunStatus status;

    @Column(name = "report_uri")
    private String reportUri;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        this.updatedAt = Instant.now();
    }

    public UUID getRunId() { return runId; }
    public void setRunId(UUID runId) { this.runId = runId; }
    public IngestRunStatus getStatus() { return status; }
    public void setStatus(IngestRunStatus status) { this.status = status; }
    public String getReportUri() { return reportUri; }
    public void setReportUri(String reportUri) { this.reportUri = reportUri; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
