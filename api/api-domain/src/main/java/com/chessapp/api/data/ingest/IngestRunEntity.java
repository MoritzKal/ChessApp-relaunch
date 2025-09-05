package com.chessapp.api.data.ingest;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

/**
 * Entity representing a single ingest run.
 */
@Entity
@Table(name = "ingest_runs")
public class IngestRunEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID runId;

    @Column(name = "username", nullable = false, length = 64)
    private String username;

    @Column(name = "status", nullable = false)
    private String status;

    @Column(name = "dataset_id")
    private String datasetId;

    @Column(name = "version")
    private String version;

    @Column(name = "files_written")
    private Long filesWritten;

    @Column(name = "report_uri")
    private String reportUri;

    @Column(name = "started_at")
    private Instant startedAt;

    @Column(name = "finished_at")
    private Instant finishedAt;

    @Column(name = "from_month", nullable = false)
    private Integer fromMonth;

    @Column(name = "to_month", nullable = false)
    private Integer toMonth;

    @PrePersist
    public void prePersist() {
        if (this.startedAt == null) {
            this.startedAt = Instant.now();
        }
        if (this.runId == null) {
            this.runId = UUID.randomUUID();
        }
    }

    public UUID getRunId() {
        return runId;
    }

    public String getUsername() { return username; }

    public void setUsername(String username) { this.username = username; }

    public void setRunId(UUID runId) {
        this.runId = runId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getReportUri() {
        return reportUri;
    }

    public void setReportUri(String reportUri) {
        this.reportUri = reportUri;
    }

    public String getDatasetId() { return datasetId; }
    public void setDatasetId(String datasetId) { this.datasetId = datasetId; }

    public String getVersion() { return version; }
    public void setVersion(String version) { this.version = version; }

    public Long getFilesWritten() { return filesWritten; }
    public void setFilesWritten(Long filesWritten) { this.filesWritten = filesWritten; }

    public Instant getStartedAt() { return startedAt; }

    public void setStartedAt(Instant startedAt) { this.startedAt = startedAt; }

    public Instant getFinishedAt() { return finishedAt; }

    public void setFinishedAt(Instant finishedAt) { this.finishedAt = finishedAt; }

    public Integer getFromMonth() { return fromMonth; }

    public void setFromMonth(Integer fromMonth) { this.fromMonth = fromMonth; }

    public Integer getToMonth() { return toMonth; }

    public void setToMonth(Integer toMonth) { this.toMonth = toMonth; }
}
