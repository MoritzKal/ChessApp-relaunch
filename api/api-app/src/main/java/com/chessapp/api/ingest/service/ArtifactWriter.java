package com.chessapp.api.ingest.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.core.sync.RequestBody;

@Component
public class ArtifactWriter {
    private final S3Client s3;
    private final ObjectMapper om;
    private final String logsBucket;
    private final String reportsBucket;
    private final String ingestPrefix;

    public ArtifactWriter(S3Client s3, ObjectMapper om,
                          @Value("${chess.ingest.s3.bucket.logs:logs}") String logsBucket,
                          @Value("${chess.ingest.s3.bucket.reports:reports}") String reportsBucket,
                          @Value("${chess.ingest.s3.prefix.ingest:ingest}") String ingestPrefix) {
        this.s3 = s3; this.om = om;
        this.logsBucket = logsBucket;
        this.reportsBucket = reportsBucket;
        this.ingestPrefix = ingestPrefix;
    }

    public String putJsonToLogs(String runId, String name, Object obj) throws Exception {
        String key = ingestPrefix + "/" + runId + "/" + name;
        byte[] data = om.writeValueAsBytes(obj);
        s3.putObject(PutObjectRequest.builder().bucket(logsBucket).key(key).build(), RequestBody.fromBytes(data));
        return "s3://" + logsBucket + "/" + key;
    }

    public String putReport(String runId, Object obj) throws Exception {
        String key = ingestPrefix + "/" + runId + "/report.json";
        byte[] data = om.writeValueAsBytes(obj);
        s3.putObject(PutObjectRequest.builder().bucket(reportsBucket).key(key).build(), RequestBody.fromBytes(data));
        return "s3://" + reportsBucket + "/" + key;
    }
}
