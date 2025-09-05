package com.chessapp.api.domain.entity;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import org.hibernate.annotations.Type;
import jakarta.persistence.*;
import com.vladmihalcea.hibernate.type.json.JsonType;

@Entity
@Table(name = "datasets")
public class Dataset {
    @Id
    private UUID id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String version;

    @Type(JsonType.class)
    @Column(columnDefinition = "jsonb")
    private Map<String, Object> filter;

    @Type(JsonType.class)
    @Column(columnDefinition = "jsonb")
    private Map<String, Object> split;

    @Column(name = "size_rows")
    private Long sizeRows;

    @Column(name = "size_bytes")
    private Long sizeBytes;

    @Column(name = "location_uri")
    private String locationUri;

    @Column(name = "created_at")
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    @PrePersist
    public void prePersist() {
        if (id == null) id = UUID.randomUUID();
        if (createdAt == null) createdAt = Instant.now();
        if (updatedAt == null) updatedAt = createdAt;
        if (filter == null) filter = Map.of();
        if (split == null) split = Map.of();
        if (version == null) version = "v0";
        if (sizeRows == null) sizeRows = 0L;
        if (sizeBytes == null) sizeBytes = 0L;
        if (name != null) name = name.trim();
    }

    @PreUpdate
    public void preUpdate() {
        if (updatedAt == null) updatedAt = Instant.now();
        if (filter == null) filter = Map.of();
        if (split == null) split = Map.of();
        if (version == null) version = "v0";
        if (sizeRows == null) sizeRows = 0L;
        if (sizeBytes == null) sizeBytes = 0L;
        if (name != null) name = name.trim();
    }

    // getters and setters
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getVersion() { return version; }
    public void setVersion(String version) { this.version = version; }
    public Map<String, Object> getFilter() { return filter; }
    public void setFilter(Map<String, Object> filter) { this.filter = filter; }
    public Map<String, Object> getSplit() { return split; }
    public void setSplit(Map<String, Object> split) { this.split = split; }
    public Long getSizeRows() { return sizeRows; }
    public void setSizeRows(Long sizeRows) { this.sizeRows = sizeRows; }
    public Long getSizeBytes() { return sizeBytes; }
    public void setSizeBytes(Long sizeBytes) { this.sizeBytes = sizeBytes; }
    public String getLocationUri() { return locationUri; }
    public void setLocationUri(String locationUri) { this.locationUri = locationUri; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
