package com.chessapp.api.ingest.entity;

import com.chessapp.api.ingest.IngestRunStatus;
import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity(name = "ingest_runs")
public class IngestRunEntity {

    @Id
    private UUID id;

    @Column(nullable = false)
    private String username;

    @Column(name = "range")
    private String range;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private IngestRunStatus status;

    @Column(name = "report_uri")
    private String reportUri;

    private String error;

    @Column(name = "started_at", nullable = false)
    private Instant startedAt;

    @Column(name = "finished_at")
    private Instant finishedAt;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getRange() { return range; }
    public void setRange(String range) { this.range = range; }

    public IngestRunStatus getStatus() { return status; }
    public void setStatus(IngestRunStatus status) { this.status = status; }

    public String getReportUri() { return reportUri; }
    public void setReportUri(String reportUri) { this.reportUri = reportUri; }

    public String getError() { return error; }
    public void setError(String error) { this.error = error; }

    public Instant getStartedAt() { return startedAt; }
    public void setStartedAt(Instant startedAt) { this.startedAt = startedAt; }

    public Instant getFinishedAt() { return finishedAt; }
    public void setFinishedAt(Instant finishedAt) { this.finishedAt = finishedAt; }
}
