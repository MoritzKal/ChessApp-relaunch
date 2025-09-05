package com.chessapp.api.chesscom.service;

import com.chessapp.api.datasets.service.DatasetCatalogService;
import com.chessapp.api.storage.MinioStorageService;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;

import java.time.YearMonth;
import java.util.List;
import java.util.UUID;

/** Orchestrates Chess.com download -> storage -> catalog upsert. */
@Service
public class ChessComIngestService {

    private static final Logger log = LoggerFactory.getLogger(ChessComIngestService.class);

    private final ChessComDownloader downloader;
    private final MinioStorageService storage;
    private final DatasetCatalogService catalog;
    private final Counter upserts;

    public ChessComIngestService(ChessComDownloader downloader,
                                 MinioStorageService storage,
                                 DatasetCatalogService catalog,
                                 MeterRegistry meterRegistry) {
        this.downloader = downloader;
        this.storage = storage;
        this.catalog = catalog;
        this.upserts = meterRegistry.counter("chs_ingest_upsert_total");
    }

    public void ingest(UUID runId, String datasetId, String username, List<YearMonth> months) {
        MDC.put("run_id", runId.toString());
        MDC.put("dataset_id", datasetId);
        MDC.put("username", username);
        try {
            for (YearMonth ym : months) {
                MDC.put("component", "download");
                byte[] data = downloader.download(username, ym);
                String key = datasetId + "/" + ym + ".pgn";
                MDC.put("component", "storage");
                storage.write(key, data);
                MDC.put("component", "catalog");
                String version = "v" + ym;
                catalog.addVersion(datasetId, version, 0L, data.length);
                upserts.increment();
            }
            log.info("ingest completed");
        } finally {
            MDC.remove("component");
            MDC.remove("username");
            MDC.remove("dataset_id");
            MDC.remove("run_id");
        }
    }
}
