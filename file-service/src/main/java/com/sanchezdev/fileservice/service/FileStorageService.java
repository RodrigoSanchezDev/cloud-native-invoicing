package com.sanchezdev.fileservice.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FileStorageService {
    @Value("${efs.base.dir}")
    private String efsBaseDir;
    @Value("${spring.cloud.aws.s3.bucket}")
    private String bucket;
    private final S3Client s3Client;

    public void saveFile(String client, String date, MultipartFile file) {
        // 1. Save on EFS
        String path = efsBaseDir + "/" + client + "/" + date;
        File dir = new File(path);
        if (!dir.exists()) dir.mkdirs();
        File dest = new File(dir, file.getOriginalFilename());
        try {
            file.transferTo(dest);
        } catch (Exception e) {
            throw new RuntimeException("Error guardando archivo en EFS", e);
        }
        // 2. Upload to S3
        PutObjectRequest putReq = PutObjectRequest.builder()
                .bucket(bucket)
                .key(client + "/" + date + "/" + file.getOriginalFilename())
                .contentType(file.getContentType())
                .build();
        s3Client.putObject(putReq, RequestBody.fromFile(dest));
    }

    public byte[] downloadFile(String key) {
        // Download always from S3
        GetObjectRequest getReq = GetObjectRequest.builder()
                .bucket(bucket)
                .key(key)
                .build();
        ResponseBytes<GetObjectResponse> resp = s3Client.getObjectAsBytes(getReq);
        return resp.asByteArray();
    }

    public void deleteFile(String key) {
        // Delete from S3
        DeleteObjectRequest deleteReq = DeleteObjectRequest.builder()
                .bucket(bucket)
                .key(key)
                .build();
        s3Client.deleteObject(deleteReq);
        
        // También eliminar de EFS si existe
        try {
            String efsPath = efsBaseDir + "/" + key;
            File efsFile = new File(efsPath);
            if (efsFile.exists()) {
                efsFile.delete();
            }
        } catch (Exception e) {
            // Log error but don't fail the operation
            System.err.println("Warning: Could not delete EFS file " + key + ": " + e.getMessage());
        }
    }

    public List<String> listFiles() {
        // List files from S3
        ListObjectsV2Request listReq = ListObjectsV2Request.builder()
                .bucket(bucket)
                .build();
        ListObjectsV2Response listResp = s3Client.listObjectsV2(listReq);
        
        return listResp.contents().stream()
                .map(S3Object::key)
                .collect(Collectors.toList());
    }
}
