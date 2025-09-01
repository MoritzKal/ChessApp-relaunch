package com.chessapp.api.service.dto;

import java.util.Map;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Schema(name = "DatasetCreateRequest")
public class DatasetCreateRequest {
    @NotBlank
    @Schema(example = "sample_ds")
    private String name;

    @NotBlank
    @Schema(example = "v1")
    private String version;

    @NotNull
    @Schema(description = "filter parameters")
    private Map<String, Object> filter;

    @NotNull
    @Schema(description = "split ratios")
    private Map<String, Object> split;

    @Schema(description = "number of rows")
    private Long sizeRows;

    public DatasetCreateRequest() {
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
}
