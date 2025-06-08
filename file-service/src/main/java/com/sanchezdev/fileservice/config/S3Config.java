package com.sanchezdev.fileservice.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.beans.factory.annotation.Value;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsSessionCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;

@Configuration
public class S3Config {
  @Bean
  public S3Client s3Client(
      @Value("${spring.cloud.aws.credentials.access-key}") String accessKey,
      @Value("${spring.cloud.aws.credentials.secret-key}") String secretKey,
      @Value("${spring.cloud.aws.region.static}") String region,
      @Value("${AWS_SESSION_TOKEN:}") String sessionToken) {
    if (sessionToken != null && !sessionToken.isEmpty()) {
      return S3Client.builder()
          .region(Region.of(region))
          .credentialsProvider(StaticCredentialsProvider.create(
              AwsSessionCredentials.create(accessKey, secretKey, sessionToken)))
          .build();
    } else {
      return S3Client.builder()
          .region(Region.of(region))
          .credentialsProvider(StaticCredentialsProvider.create(
              AwsBasicCredentials.create(accessKey, secretKey)))
          .build();
    }
  }
}
