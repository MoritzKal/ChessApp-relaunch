package com.chessapp.api.chesscom.service;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.YearMonth;

/** Downloader wrapper around {@link ChessComService}. */
@Service
public class ChessComDownloader {

    private static final Logger log = LoggerFactory.getLogger(ChessComDownloader.class);

    private final ChessComService service;
    private final Counter downloads;

    public ChessComDownloader(ChessComService service, MeterRegistry meterRegistry) {
        this.service = service;
        this.downloads = meterRegistry.counter("chs_ingest_download_total");
    }

    public byte[] download(String user, YearMonth ym) {
        log.info("download user={} ym={}", user, ym);
        byte[] data = service.downloadPgn(user, ym);
        downloads.increment();
        return data;
    }
}
