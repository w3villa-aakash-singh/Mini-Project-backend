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

    private final S3Client s3Client;

    @Value("${app.supabase.bucket-name}")
    private String bucketName;

    // Replace 'zongeyrdnaapvpcvkpep' if it changes, but this matches your YML
    private final String SUPABASE_PUBLIC_URL = "https://zongeyrdnaapvpcvkpep.supabase.co/storage/v1/object/public/";

    public String uploadImage(MultipartFile file, String fileName) throws IOException {
        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                .bucket(bucketName)
                .key(fileName)
                .contentType(file.getContentType())
                .build();

        s3Client.putObject(putObjectRequest,
                RequestBody.fromInputStream(file.getInputStream(), file.getSize()));

        // Returns: https://zongeyrdnaapvpcvkpep.supabase.co/storage/v1/object/public/user-profile/filename.png
        return SUPABASE_PUBLIC_URL + bucketName + "/" + fileName;
    }
}