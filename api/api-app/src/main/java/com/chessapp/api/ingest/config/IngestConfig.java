package com.chessapp.api.ingest.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.ThreadLocalRandom;
import java.util.function.LongSupplier;

@Configuration
public class IngestConfig {

    @Bean
    public LongSupplier ingestDelaySupplier() {
        return () -> ThreadLocalRandom.current().nextLong(50, 150);
    }
}
