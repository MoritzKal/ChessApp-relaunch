package com.chessapp.api.dataset.dto;

import java.time.Instant;
import java.util.UUID;

public class DatasetResponse {
    private UUID id;
    private String name;
    private String version;
    private String filterJson;
    private String splitJson;
    private Long sizeRows;
    private String locationUri;
    private Instant createdAt;

    public DatasetResponse() {
    }

    public DatasetResponse(UUID id, String name, String version, String filterJson, String splitJson,
                           Long sizeRows, String locationUri, Instant createdAt) {
        this.id = id;
        this.name = name;
        this.version = version;
        this.filterJson = filterJson;
        this.splitJson = splitJson;
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
    public String getFilterJson() { return filterJson; }
    public void setFilterJson(String filterJson) { this.filterJson = filterJson; }
    public String getSplitJson() { return splitJson; }
    public void setSplitJson(String splitJson) { this.splitJson = splitJson; }
    public Long getSizeRows() { return sizeRows; }
    public void setSizeRows(Long sizeRows) { this.sizeRows = sizeRows; }
    public String getLocationUri() { return locationUri; }
    public void setLocationUri(String locationUri) { this.locationUri = locationUri; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
