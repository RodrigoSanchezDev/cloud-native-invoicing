package com.sanchezdev.fileservice.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.File;
import java.io.IOException;

@Service
@RequiredArgsConstructor
public class FileStorageService {
    @Value("${efs.base.dir}")
    private String efsBaseDir;
    @Value("${spring.cloud.aws.s3.bucket}")
    private String bucket;
    private final S3Client s3Client;

    public void saveFile(String client, String date, MultipartFile file) {
        // 1. Guardar en EFS
        String path = efsBaseDir + "/" + client + "/" + date;
        File dir = new File(path);
        if (!dir.exists()) dir.mkdirs();
        File dest = new File(dir, file.getOriginalFilename());
        try {
            file.transferTo(dest);
        } catch (Exception e) {
            throw new RuntimeException("Error guardando archivo en EFS", e);
        }
        // 2. Subir a S3
        PutObjectRequest putReq = PutObjectRequest.builder()
                .bucket(bucket)
                .key(client + "/" + date + "/" + file.getOriginalFilename())
                .contentType(file.getContentType())
                .build();
        s3Client.putObject(putReq, RequestBody.fromFile(dest));
    }

    public byte[] downloadFile(String key) {
        // Descargar siempre desde S3
        GetObjectRequest getReq = GetObjectRequest.builder()
                .bucket(bucket)
                .key(key)
                .build();
        ResponseBytes<GetObjectResponse> resp = s3Client.getObjectAsBytes(getReq);
        return resp.asByteArray();
    }
}
