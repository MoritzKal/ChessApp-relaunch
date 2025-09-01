package com.chessapp.api.service.dto;

import java.util.Map;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

public class CreateDatasetRequest {
    @NotBlank
    private String name;
    private String version;
    private Map<String, Object> filter;
    private Map<String, Object> split;
    @Positive
    private Long sizeRows;
    private String locationUri;

    public CreateDatasetRequest() {
    }

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
}
