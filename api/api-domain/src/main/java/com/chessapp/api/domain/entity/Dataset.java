package com.chessapp.api.domain.entity;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import org.hibernate.annotations.Type;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "datasets")
public class Dataset {
    @Id
    private UUID id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String version;

    @Type(org.hibernate.type.JsonType.class)
    @Column(columnDefinition = "jsonb")
    private Map<String, Object> filter;

    @Type(org.hibernate.type.JsonType.class)
    @Column(columnDefinition = "jsonb")
    private Map<String, Object> split;

    @Column(name = "size_rows")
    private Long sizeRows;

    @Column(name = "location_uri")
    private String locationUri;

    @Column(name = "created_at")
    private Instant createdAt;

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
    public String getLocationUri() { return locationUri; }
    public void setLocationUri(String locationUri) { this.locationUri = locationUri; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
