package com.chessapp.api.service.dto;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public class DatasetResponse {
    private UUID id;
    private String name;
    private String version;
    private Map<String, Object> filter;
    private Map<String, Object> split;
    private Long sizeRows;
    private String locationUri;
    private Instant createdAt;

    public DatasetResponse() {
    }

    public DatasetResponse(UUID id,
                           String name,
                           String version,
                           Map<String, Object> filter,
                           Map<String, Object> split,
                           Long sizeRows,
                           String locationUri,
                           Instant createdAt) {
        this.id = id;
        this.name = name;
        this.version = version;
        this.filter = filter;
        this.split = split;
        this.sizeRows = sizeRows;
        this.locationUri = locationUri;
        this.createdAt = createdAt;
    }

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
