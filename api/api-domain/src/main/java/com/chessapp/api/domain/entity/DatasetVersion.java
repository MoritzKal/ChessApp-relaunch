package com.chessapp.api.domain.entity;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.*;

@Entity
@Table(name = "dataset_versions", uniqueConstraints = @UniqueConstraint(columnNames = {"dataset_id", "version"}))
public class DatasetVersion {
    @Id
    private UUID id;

    @Column(name = "dataset_id", nullable = false)
    private UUID datasetId;

    @Column(nullable = false)
    private String version;

    @Column(name = "rows")
    private Long rows;

    @Column(name = "size_bytes")
    private Long sizeBytes;

    @Column(name = "created_at")
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    @PrePersist
    public void prePersist() {
        if (id == null) {
            id = UUID.randomUUID();
        }
        if (createdAt == null) {
            createdAt = Instant.now();
        }
        if (updatedAt == null) {
            updatedAt = createdAt;
        }
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = Instant.now();
    }

    // getters and setters
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getDatasetId() { return datasetId; }
    public void setDatasetId(UUID datasetId) { this.datasetId = datasetId; }
    public String getVersion() { return version; }
    public void setVersion(String version) { this.version = version; }
    public Long getRows() { return rows; }
    public void setRows(Long rows) { this.rows = rows; }
    public Long getSizeBytes() { return sizeBytes; }
    public void setSizeBytes(Long sizeBytes) { this.sizeBytes = sizeBytes; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
