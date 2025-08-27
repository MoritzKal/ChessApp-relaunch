package com.chessapp.api.config;

import java.util.concurrent.atomic.AtomicLong;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
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

    @Bean
    public Counter datasetBuildCounter(MeterRegistry registry) {
        return Counter.builder("chs_dataset_build_total").register(registry);
    }

    @Bean
    public AtomicLong datasetRowsGauge(MeterRegistry registry) {
        AtomicLong value = new AtomicLong(0);
        Gauge.builder("chs_dataset_rows", value, AtomicLong::get).register(registry);
        return value;
    }
}
