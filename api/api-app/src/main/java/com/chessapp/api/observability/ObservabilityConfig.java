package com.chessapp.api.observability;

import java.util.concurrent.atomic.AtomicLong;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.config.MeterFilter;
import io.micrometer.core.instrument.Tags;

@Configuration
public class ObservabilityConfig {

    @Bean
    public MeterFilter apiCommonTags() {
        return MeterFilter.commonTags(Tags.of("application", "api"));
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

    @Bean
    public Counter ingestJobsCounter(MeterRegistry registry) {
        return Counter.builder("chs_ingest_jobs_total").register(registry);
    }

    @Bean
    public Counter ingestGamesCounter(MeterRegistry registry) {
        return Counter.builder("chs_ingest_games_total").register(registry);
    }

    @Bean
    public Counter ingestPositionsCounter(MeterRegistry registry) {
        return Counter.builder("chs_ingest_positions_total").register(registry);
    }

    @Bean
    public Timer ingestDurationTimer(MeterRegistry registry) {
        return Timer.builder("chs_ingest_duration_seconds")
                .publishPercentiles(0.5, 0.95)
                .publishPercentileHistogram()
                .register(registry);
    }
}

