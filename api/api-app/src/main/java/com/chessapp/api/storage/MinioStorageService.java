package com.chessapp.api.storage;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

/** Simple wrapper around S3/Minio for storing ingest artifacts. */
@Service
public class MinioStorageService {

    private static final Logger log = LoggerFactory.getLogger(MinioStorageService.class);

    private final S3Client s3;
    private final String bucket;
    private final Counter writes;

    public MinioStorageService(S3Client s3,
                               @Value("${chess.storage.bucket:datasets}") String bucket,
                               MeterRegistry meterRegistry) {
        this.s3 = s3;
        this.bucket = bucket;
        this.writes = meterRegistry.counter("chs_ingest_write_total");
    }

    public void write(String key, byte[] data) {
        log.info("write key={} size={}B", key, data.length);
        PutObjectRequest req = PutObjectRequest.builder()
                .bucket(bucket)
                .key(key)
                .contentType("application/octet-stream")
                .build();
        s3.putObject(req, RequestBody.fromBytes(data));
        writes.increment();
    }

    /** Write to an explicit bucket and content type (e.g., PGN). */
    public void write(String bucket, String key, byte[] data, String contentType) {
        log.info("write bucket={} key={} size={}B contentType={}", bucket, key, data.length, contentType);
        PutObjectRequest req = PutObjectRequest.builder()
                .bucket(bucket)
                .key(key)
                .contentType(contentType)
                .build();
        s3.putObject(req, RequestBody.fromBytes(data));
        writes.increment();
    }

    public byte[] read(String bucket, String key) {
        try {
            var obj = s3.getObject(b -> b.bucket(bucket).key(key));
            return obj.readAllBytes();
        } catch (Exception e) {
            log.warn("read failed bucket={} key={} error={}", bucket, key, e.toString());
            return new byte[0];
        }
    }

    public byte[] readUri(String uri) {
        // expects s3://bucket/key
        if (uri == null || !uri.startsWith("s3://")) return new byte[0];
        String no = uri.substring("s3://".length());
        int slash = no.indexOf('/');
        if (slash <= 0) return new byte[0];
        String bucket = no.substring(0, slash);
        String key = no.substring(slash + 1);
        return read(bucket, key);
    }
}
