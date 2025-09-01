package com.chessapp.api;

import org.mockito.Mockito;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

import static org.mockito.ArgumentMatchers.any;

@TestConfiguration
public class TestS3Config {

  @Bean
  @Primary
  public S3Client s3ClientMock() {
    S3Client mock = Mockito.mock(S3Client.class);

    // putObject: immer "erfolgreich"
    Mockito.when(mock.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
      .thenReturn(PutObjectResponse.builder().eTag("unit-test").build());

    // headBucket/createBucket/getObject etc. optional stubben – keine Exceptions:
    Mockito.when(mock.headBucket(any(HeadBucketRequest.class)))
      .thenReturn(HeadBucketResponse.builder().build());
    Mockito.when(mock.createBucket(any(CreateBucketRequest.class)))
      .thenReturn(CreateBucketResponse.builder().build());
    // Falls euer Code irgendwo getObject nutzt, hier ein harmless Stub:
    Mockito.when(mock.getObject(any(GetObjectRequest.class)))
      .thenThrow(NoSuchKeyException.builder().message("not used in tests").build());

    return mock;
  }
}
