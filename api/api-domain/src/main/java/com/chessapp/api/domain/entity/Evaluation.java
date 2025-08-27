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
@Table(name = "evaluations")
public class Evaluation {
    @Id
    private UUID id;

    @Column(name = "model_id")
    private UUID modelId;

    @Column(name = "baseline_model_id")
    private UUID baselineModelId;

    @Type(org.hibernate.type.JsonType.class)
    @Column(name = "metric_suite", columnDefinition = "jsonb")
    private Map<String, Object> metricSuite;

    @Column(name = "report_uri")
    private String reportUri;

    @Column(name = "created_at")
    private Instant createdAt;

    // getters and setters
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getModelId() { return modelId; }
    public void setModelId(UUID modelId) { this.modelId = modelId; }
    public UUID getBaselineModelId() { return baselineModelId; }
    public void setBaselineModelId(UUID baselineModelId) { this.baselineModelId = baselineModelId; }
    public Map<String, Object> getMetricSuite() { return metricSuite; }
    public void setMetricSuite(Map<String, Object> metricSuite) { this.metricSuite = metricSuite; }
    public String getReportUri() { return reportUri; }
    public void setReportUri(String reportUri) { this.reportUri = reportUri; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
