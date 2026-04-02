package com.w3villa.mini_project_backend.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;

import java.net.URI;
@Configuration
public class SupabaseConfig {

    @Value("${app.supabase.endpoint}")
    private String endpoint;

    @Value("${app.supabase.access-key}")
    private String accessKey;

    @Value("${app.supabase.secret-key}")
    private String secretKey;

    @Value("${app.supabase.region}")
    private String region;

    @Bean
    public S3Client s3Client() {
        return S3Client.builder()
                .endpointOverride(URI.create(endpoint))
                .forcePathStyle(true) // CRITICAL: Supabase needs this
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(accessKey, secretKey)))
                .region(Region.of(region))
                .build();
    }
}