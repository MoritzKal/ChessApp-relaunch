package com.chessapp.api.domain.entity;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "evaluations")
public class Evaluation {
    @Id
    private UUID id;

    @Column(name = "model_id")
    private UUID modelId;

    @Column(name = "baseline_model_id")
    private UUID baselineModelId;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "metric_suite", columnDefinition = "jsonb", nullable = false)
    private Map<String, Object> metricSuite;

    @Column(name = "report_uri")
    private String reportUri;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @PrePersist
    void prePersist() {
        if (id == null) id = UUID.randomUUID();
        if (createdAt == null) createdAt = Instant.now();
        if (metricSuite == null) metricSuite = new java.util.HashMap<>();
    }

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
