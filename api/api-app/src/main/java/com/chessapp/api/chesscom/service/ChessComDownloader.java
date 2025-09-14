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
    private final Counter gamesCounter;
    private final Counter bytesCounter;

    public ChessComDownloader(ChessComService service, MeterRegistry meterRegistry) {
        this.service = service;
        this.downloads = meterRegistry.counter("chs_ingest_download_total");
        this.gamesCounter = meterRegistry.counter("chs_ingest_games_total");
        this.bytesCounter = meterRegistry.counter("chs_ingest_bytes_total");
    }

    public byte[] download(String user, YearMonth ym) {
        log.info("download user={} ym={}", user, ym);
        byte[] data = service.downloadPgn(user, ym);
        downloads.increment();
        return data;
    }

    /**
     * Download a month's PGN for a user and return both bytes and a simple game count.
     * The heuristic counts occurrences of the PGN tag line prefix "[Event \"".
     */
    public MonthDownload downloadMonth(String user, YearMonth ym) {
        byte[] data = download(user, ym);
        int games = 0;
        // Count games by scanning the bytes for "[Event \""
        for (int i = 0; i < data.length - 8; i++) {
            if (data[i] == '['
                    && i + 8 < data.length
                    && data[i + 1] == 'E'
                    && data[i + 2] == 'v'
                    && data[i + 3] == 'e'
                    && data[i + 4] == 'n'
                    && data[i + 5] == 't'
                    && data[i + 6] == ' '
                    && data[i + 7] == '"') {
                games++;
            }
        }
        gamesCounter.increment(games);
        bytesCounter.increment(data.length);
        log.info("downloaded month user={} ym={} bytes={} games={}", user, ym, data.length, games);
        return new MonthDownload(data, games);
    }

    public record MonthDownload(byte[] bytes, int games) {}
}
