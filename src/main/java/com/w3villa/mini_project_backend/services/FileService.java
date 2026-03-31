package com.w3villa.mini_project_backend.services;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.IOException;

@Service
@RequiredArgsConstructor
public class FileService {

    private final S3Client s3Client; // This uses the Bean from your StorjConfig

    @Value("${app.storj.bucket-name}")
    private String bucketName;

    @Value("${app.storj.endpoint}")
    private String storjEndpoint;

    public String uploadImage(MultipartFile file, String fileName) throws IOException {
        // 1. Prepare the S3 Put Request
        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                .bucket(bucketName)
                .key(fileName)
                .contentType(file.getContentType())
                .build();

        // 2. Upload the bytes to Storj
        s3Client.putObject(putObjectRequest,
                RequestBody.fromInputStream(file.getInputStream(), file.getSize()));

        // 3. Construct the Public URL for your React Frontend
        // Example: https://gateway.storjshare.io/user-profile/profiles/123_456.png
        return String.format("%s/%s/%s", storjEndpoint, bucketName, fileName);
    }
}