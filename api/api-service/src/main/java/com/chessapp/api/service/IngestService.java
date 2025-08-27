package com.chessapp.api.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;

@Service
public class IngestService {

    private static final Logger log = LoggerFactory.getLogger(IngestService.class);
    @Autowired
    private final MeterRegistry meterRegistry;
    private Counter ingestJobs;

    public IngestService(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    public void startImport() {
        ingestJobs.increment();
        meterRegistry.counter("chs_ingest_jobs_total").increment();
        log.info("ingest job started");
    }

    @PostConstruct
    void initMetrics() {
        this.ingestJobs = Counter.builder("chs_ingest_jobs_total")
                .description("Number of ingest jobs triggered")
                .register(meterRegistry);
    }
}
