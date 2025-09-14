package com.chessapp.api.chesscom;

import com.chessapp.api.chesscom.service.ChessComDownloader;
import com.chessapp.api.chesscom.service.ChessComService;
import io.micrometer.core.instrument.MeterRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import software.amazon.awssdk.services.s3.S3Client;

import java.time.YearMonth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@SpringBootTest
@ActiveProfiles("test")
class DownloaderTest {

    @Autowired ChessComDownloader downloader;
    @Autowired MeterRegistry meterRegistry;
    @MockitoBean ChessComService service;
    @MockitoBean S3Client s3;

    @Test
    void download_increments_metric() {
        YearMonth ym = YearMonth.of(2023, 9);
        when(service.downloadPgn("bob", ym)).thenReturn("pgn".getBytes());
        byte[] data = downloader.download("bob", ym);
        assertThat(data).isEqualTo("pgn".getBytes());
        assertThat(meterRegistry.counter("chs_ingest_download_total").count()).isEqualTo(1.0);
    }

    @Test
    void downloadMonth_counts_games() {
        YearMonth ym = YearMonth.of(2024, 1);
        String pgn = """
                [Event \"Game1\"]\n[Site \"-\"]\n\n1. e4 e5 1/2-1/2\n\n[Event \"Game2\"]\n[Site \"-\"]\n\n1. d4 d5 1-0\n""";
        when(service.downloadPgn("alice", ym)).thenReturn(pgn.getBytes());
        var dl = downloader.downloadMonth("alice", ym);
        assertThat(dl.bytes()).isNotEmpty();
        assertThat(dl.games()).isGreaterThan(0);
        assertThat(meterRegistry.counter("chs_ingest_games_total").count()).isGreaterThan(0.0);
        assertThat(meterRegistry.counter("chs_ingest_bytes_total").count()).isGreaterThan(0.0);
    }
}
