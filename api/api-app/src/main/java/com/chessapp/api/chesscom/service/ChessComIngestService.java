package com.chessapp.api.chesscom.service;

import com.chessapp.api.datasets.service.DatasetCatalogService;
import com.chessapp.api.data.ingest.IngestRunRepository;
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
    private final IngestRunRepository ingestRunRepository;
    private final Counter upserts;
    private final Counter filesWrittenCounter;

    public ChessComIngestService(ChessComDownloader downloader,
                                 MinioStorageService storage,
                                 DatasetCatalogService catalog,
                                 IngestRunRepository ingestRunRepository,
                                 MeterRegistry meterRegistry) {
        this.downloader = downloader;
        this.storage = storage;
        this.catalog = catalog;
        this.ingestRunRepository = ingestRunRepository;
        this.upserts = meterRegistry.counter("chs_ingest_upsert_total");
        this.filesWrittenCounter = meterRegistry.counter("chs_ingest_files_written");
    }

    public void ingest(UUID runId, String datasetId, String username, List<YearMonth> months) {
        MDC.put("run_id", runId.toString());
        MDC.put("dataset_id", datasetId);
        MDC.put("username", username);
        try {
            for (YearMonth ym : months) {
                MDC.put("component", "download");
                var dl = downloader.downloadMonth(username, ym);
                byte[] data = dl.bytes();
                int games = dl.games();
                String version = "v" + ym; // ym prints as YYYY-MM

                // Storage: datasets/<datasetId>/<version>/raw.pgn (bucket "datasets")
                String key = datasetId + "/" + version + "/raw.pgn";
                MDC.put("component", "storage");
                storage.write("datasets", key, data, "application/x-chess-pgn");

                // Catalog upsert with rows = games, sizeBytes = real bytes
                MDC.put("component", "catalog");
                catalog.addVersion(datasetId, version, games, data.length);
                upserts.increment();

                // Update run.filesWritten and metrics
                ingestRunRepository.findById(runId).ifPresent(run -> {
                    Long curr = run.getFilesWritten() == null ? 0L : run.getFilesWritten();
                    run.setFilesWritten(curr + 1);
                    ingestRunRepository.save(run);
                });
                filesWrittenCounter.increment();

                // Structured log
                log.info("ingest step component=ingest datasetId={} username={} ym={} version={} bytes={} games={}",
                        datasetId, username, ym, version, data.length, games);
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
