package com.chessapp.api.service.dto;

import java.util.Map;

public class DatasetCreateRequest {
    private String name;
    private String version;
    private Map<String, Object> filter;
    private Map<String, Object> split;
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
