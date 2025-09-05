package com.chessapp.api.chesscom;

import com.chessapp.api.chesscom.service.ChessComDownloader;
import com.chessapp.api.chesscom.service.ChessComService;
import io.micrometer.core.instrument.MeterRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import software.amazon.awssdk.services.s3.S3Client;

import java.time.YearMonth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@SpringBootTest
@ActiveProfiles("test")
class DownloaderTest {

    @Autowired ChessComDownloader downloader;
    @Autowired MeterRegistry meterRegistry;
    @MockBean ChessComService service;
    @MockBean S3Client s3;

    @Test
    void download_increments_metric() {
        YearMonth ym = YearMonth.of(2023, 9);
        when(service.downloadPgn("bob", ym)).thenReturn("pgn".getBytes());
        byte[] data = downloader.download("bob", ym);
        assertThat(data).isEqualTo("pgn".getBytes());
        assertThat(meterRegistry.counter("chs_ingest_download_total").count()).isEqualTo(1.0);
    }
}
