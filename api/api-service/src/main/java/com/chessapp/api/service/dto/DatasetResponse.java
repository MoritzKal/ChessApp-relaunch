package com.chessapp.api.service.dto;

import java.time.Instant;
import java.util.UUID;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "DatasetResponse")
public class DatasetResponse {
    @Schema(example = "3fa85f64-5717-4562-b3fc-2c963f66afa6")
    private UUID id;
    private String name;
    private String version;
    @Schema(description = "number of rows")
    private Long sizeRows;
    @Schema(description = "S3 URI of manifest")
    private String locationUri;
    private Instant createdAt;

    public DatasetResponse() {
    }

    public DatasetResponse(UUID id, String name, String version, Long sizeRows, String locationUri, Instant createdAt) {
        this.id = id;
        this.name = name;
        this.version = version;
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
    public Long getSizeRows() { return sizeRows; }
    public void setSizeRows(Long sizeRows) { this.sizeRows = sizeRows; }
    public String getLocationUri() { return locationUri; }
    public void setLocationUri(String locationUri) { this.locationUri = locationUri; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
