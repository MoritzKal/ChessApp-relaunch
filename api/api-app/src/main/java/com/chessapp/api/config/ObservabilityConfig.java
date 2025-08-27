package com.chessapp.api.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.boot.actuate.autoconfigure.metrics.MeterRegistryCustomizer;

@Configuration
public class ObservabilityConfig {

    @Bean
    public MeterRegistryCustomizer<MeterRegistry> metricsCommonTags(
            @Value("${CHESS_USERNAME:M3NG00S3}") String defaultUser) {
        return registry -> registry.config().commonTags(
                "application", "api",
                "component", "api",
                "username",  defaultUser
        );
    }
}
