package com.sanchezdev.fileservice.service;

import java.io.File;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import lombok.RequiredArgsConstructor;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Response;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Object;

@Service
@RequiredArgsConstructor
public class FileStorageService {
    @Value("${efs.base.dir}")
    private String efsBaseDir;
    @Value("${spring.cloud.aws.s3.bucket}")
    private String bucket;
    private final S3Client s3Client;

    public String saveFile(String client, String date, MultipartFile file) {
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
        String key = client + "/" + date + "/" + file.getOriginalFilename();
        PutObjectRequest putReq = PutObjectRequest.builder()
                .bucket(bucket)
                .key(key)
                .contentType(file.getContentType())
                .build();
        s3Client.putObject(putReq, RequestBody.fromFile(dest));
        return key;
    }

    public String savePdfFile(String fileName, byte[] content) {
        try {
            // Generate key with current date structure
            String currentDate = java.time.LocalDate.now().toString();
            String key = "invoices/" + currentDate + "/" + fileName;
            
            // 1. Save on EFS
            String path = efsBaseDir + "/invoices/" + currentDate;
            File dir = new File(path);
            if (!dir.exists()) dir.mkdirs();
            File dest = new File(dir, fileName);
            
            try (java.io.FileOutputStream fos = new java.io.FileOutputStream(dest)) {
                fos.write(content);
            }
            
            // 2. Upload to S3
            PutObjectRequest putReq = PutObjectRequest.builder()
                    .bucket(bucket)
                    .key(key)
                    .contentType("application/pdf")
                    .build();
            s3Client.putObject(putReq, RequestBody.fromBytes(content));
            
            System.out.println("PDF file saved successfully to S3 with key: " + key);
            return key;
        } catch (Exception e) {
            throw new RuntimeException("Error saving PDF file", e);
        }
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
    
    /**
     * Verifica si un archivo existe en EFS y retorna información detallada
     */
    public FileVerificationResult verifyFileInEFS(String s3Key) {
        try {
            // Convertir S3 key al path de EFS
            String efsPath = efsBaseDir + "/" + s3Key;
            File efsFile = new File(efsPath);
            
            FileVerificationResult result = new FileVerificationResult();
            result.s3Key = s3Key;
            result.efsPath = efsPath;
            result.exists = efsFile.exists();
            
            if (result.exists) {
                result.size = efsFile.length();
                result.lastModified = java.time.Instant.ofEpochMilli(efsFile.lastModified()).toString();
                result.readable = efsFile.canRead();
                result.status = "SUCCESS";
                result.message = "File verified successfully in EFS";
            } else {
                result.size = 0;
                result.lastModified = null;
                result.readable = false;
                result.status = "NOT_FOUND";
                result.message = "File not found in EFS";
            }
            
            result.timestamp = java.time.LocalDateTime.now().toString();
            return result;
            
        } catch (Exception e) {
            FileVerificationResult result = new FileVerificationResult();
            result.s3Key = s3Key;
            result.efsPath = efsBaseDir + "/" + s3Key;
            result.exists = false;
            result.size = 0;
            result.lastModified = null;
            result.readable = false;
            result.status = "ERROR";
            result.message = "Error verifying file: " + e.getMessage();
            result.timestamp = java.time.LocalDateTime.now().toString();
            return result;
        }
    }
    
    /**
     * Clase para el resultado de verificación de archivos
     */
    public static class FileVerificationResult {
        public String s3Key;
        public String efsPath;
        public boolean exists;
        public long size;
        public String lastModified;
        public boolean readable;
        public String status;
        public String message;
        public String timestamp;
    }
}
