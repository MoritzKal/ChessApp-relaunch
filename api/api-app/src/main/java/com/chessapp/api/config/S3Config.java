package com.chessapp.api.config;

import java.net.URI;
import java.time.Duration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;

@Configuration
public class S3Config {

    @Bean
    public S3Client s3Client(
            @Value("${ML_S3_ENDPOINT:http://minio:9000}") String endpoint,
            @Value("${AWS_REGION:us-east-1}") String region
    ) {
        AwsCredentialsProvider creds = DefaultCredentialsProvider.create();
        String clean = endpoint.endsWith("/") ? endpoint.substring(0, endpoint.length()-1) : endpoint;

        return S3Client.builder()
                .credentialsProvider(creds)
                .endpointOverride(URI.create(clean))
                .region(Region.of(region))
                .serviceConfiguration(S3Configuration.builder()
                        .pathStyleAccessEnabled(true)       // wichtig für MinIO
                        .checksumValidationEnabled(false)   // vermeidet CRC-Mismatch bei MinIO
                        .build())
                .overrideConfiguration(c -> c
                        .apiCallAttemptTimeout(Duration.ofSeconds(10))
                        .apiCallTimeout(Duration.ofSeconds(30)))
                .build();
    }

}
