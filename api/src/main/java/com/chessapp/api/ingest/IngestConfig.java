package com.chessapp.api.ingest;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Supplier;

@Configuration
@EnableAsync
public class IngestConfig {

    @Bean(name = {"taskExecutor", "ingestExecutor"})
    public ThreadPoolTaskExecutor taskExecutor() {
        ThreadPoolTaskExecutor ex = new ThreadPoolTaskExecutor();
        ex.setCorePoolSize(4);
        ex.setMaxPoolSize(8);
        ex.setQueueCapacity(100);
        ex.setThreadNamePrefix("ingest-");
        ex.initialize();
        return ex;
    }

    @Bean
    public Supplier<Long> ingestSleep() {
        return () -> ThreadLocalRandom.current().nextLong(50, 150);
    }
}
