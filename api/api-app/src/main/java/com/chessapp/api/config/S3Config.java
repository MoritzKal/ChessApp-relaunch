package com.chessapp.api.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.client.config.ClientOverrideConfiguration;
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;

import java.net.URI;

@Configuration
public class S3Config {

    @Bean
    public S3Client s3Client(
            @Value("${S3_ENDPOINT:${s3.endpoint:http://minio:9000}}") String endpoint,
            @Value("${S3_REGION:${s3.region:us-east-1}}") String region,
            @Value("${S3_ACCESS_KEY:${s3.accessKey:${AWS_ACCESS_KEY_ID:chs_minio}}}") String accessKey,
            @Value("${S3_SECRET_KEY:${s3.secretKey:${AWS_SECRET_ACCESS_KEY:chs_minio_password_change_me}}}") String secretKey
    ) {
        return S3Client.builder()
                .httpClientBuilder(UrlConnectionHttpClient.builder())
                .endpointOverride(URI.create(endpoint))
                .region(Region.of(region))
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(accessKey, secretKey)))
                .serviceConfiguration(S3Configuration.builder().pathStyleAccessEnabled(true).build())
                .overrideConfiguration(ClientOverrideConfiguration.builder().build())
                .build();
    }
}
