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

    // Rely on management.metrics.tags.* from application.yml for common tags to avoid duplication.
    // (Previously added application tag here caused duplicate label keys on Prometheus scrape.)

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
    public Counter ingestGamesCounter(MeterRegistry registry) {
        return Counter.builder("chs_ingest_games_total").register(registry);
    }

    @Bean
    public Counter ingestPositionsCounter(MeterRegistry registry) {
        return Counter.builder("chs_ingest_positions_total").register(registry);
    }

    @Bean
    public Counter ingestSkippedCounter(MeterRegistry registry) {
        return Counter.builder("chs_ingest_skipped_total")
                .description("Number of games skipped due to duplicate (platform + game_id_ext)")
                .register(registry);
    }

    @Bean
    public Timer ingestDurationTimer(MeterRegistry registry) {
        return Timer.builder("chs_ingest_duration_seconds")
                .publishPercentiles(0.5, 0.95)
                .publishPercentileHistogram()
                .register(registry);
    }

    @Bean
    public Counter positionsLegalMovesCounter(MeterRegistry registry) {
        return Counter.builder("chs_positions_legal_moves_total")
                .description("Sum of legal moves across positions")
                .register(registry);
    }
}

