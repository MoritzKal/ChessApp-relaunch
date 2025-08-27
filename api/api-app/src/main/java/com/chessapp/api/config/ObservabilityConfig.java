package com.chessapp.api.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.MeterRegistryCustomizer;

@Configuration
public class ObservabilityConfig {

    @Bean
    MeterRegistryCustomizer<MeterRegistry> metricsCommonTags(
            @Value("${chs.default-username:M3NG00S3}") String username) {
        return registry -> registry.config().commonTags(
                "application", "api",
                "component", "api",
                "username", username);
    }

    @Bean
    Counter ingestJobsCounter(MeterRegistry registry) {
        return Counter.builder("chs_ingest_jobs_total").register(registry);
    }

    @Bean
    Counter ingestGamesCounter(MeterRegistry registry) {
        return Counter.builder("chs_ingest_games_total").register(registry);
    }

    @Bean
    Timer predictLatencyTimer(MeterRegistry registry) {
        return Timer.builder("chs_predict_latency_ms").register(registry);
    }
}
