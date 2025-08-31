package com.chessapp.api.dataset.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import com.chessapp.api.dataset.validation.ValidJson;

public class CreateDatasetRequest {
    @NotBlank
    @Size(max = 100)
    private String name;

    @NotBlank
    @Size(max = 50)
    private String version;

    @Size(max = 2048)
    @ValidJson
    private String filterJson;

    @Size(max = 2048)
    @ValidJson
    private String splitJson;

    private Long sizeRows;

    @NotBlank
    private String locationUri;

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
}
