package com.chessapp.api.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import io.micrometer.core.instrument.MeterRegistry;

@Service
public class IngestService {

    private static final Logger log = LoggerFactory.getLogger(IngestService.class);
    private final MeterRegistry meterRegistry;

    public IngestService(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    public void startImport() {
        meterRegistry.counter("chs_ingest_jobs_total").increment();
        log.info("ingest job started");
    }
}
