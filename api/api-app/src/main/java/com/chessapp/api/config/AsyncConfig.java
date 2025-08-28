package com.chessapp.api.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

@Configuration
@EnableAsync(proxyTargetClass = true) // erzwingt Klassenbasierte Proxies (CGLIB)
public class AsyncConfig implements AsyncConfigurer {

    @Bean(name = {"ingestExecutor", "taskExecutor"}) // alias 'taskExecutor' als Default
    public ThreadPoolTaskExecutor ingestExecutor() {
        ThreadPoolTaskExecutor ex = new ThreadPoolTaskExecutor();
        ex.setCorePoolSize(4);
        ex.setMaxPoolSize(8);
        ex.setQueueCapacity(200);
        ex.setThreadNamePrefix("ingest-");
        ex.initialize();
        return ex;
    }

    @Override
    public Executor getAsyncExecutor() {
        // setzt den Default-Executor für alle @Async-Methoden
        return ingestExecutor();
    }
}
