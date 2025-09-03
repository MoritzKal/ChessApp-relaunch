package com.chessapp.api.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "ratelimit")
public class RateLimitConfig {
    /** requests per minute for self-play routes */
    private int selfplayPerMin = 60;
    /** requests per minute for evaluation routes */
    private int evaluationPerMin = 60;

    public int getSelfplayPerMin() { return selfplayPerMin; }
    public void setSelfplayPerMin(int selfplayPerMin) { this.selfplayPerMin = selfplayPerMin; }
    public int getEvaluationPerMin() { return evaluationPerMin; }
    public void setEvaluationPerMin(int evaluationPerMin) { this.evaluationPerMin = evaluationPerMin; }
}
