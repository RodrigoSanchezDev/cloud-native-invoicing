package com.sanchezdev.fileservice.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;
import software.amazon.awssdk.core.sync.RequestBody;

import java.io.*;
import java.nio.file.*;
import java.util.List;
import java.util.stream.Collectors;

@Service @RequiredArgsConstructor
public class FileStorageService {
  private final S3Client s3;
  @Value("${aws.s3.bucket}")   private String bucket;
  @Value("${efs.base.dir}")    private String baseDir;

  public void save(String client, String date, String filename, byte[] data) throws IOException {
    Path dir = Paths.get(baseDir, client, date);
    Files.createDirectories(dir);
    Path file = dir.resolve(filename);
    Files.write(file, data);

    String key = client + "/" + date + "/" + filename;
    s3.putObject(PutObjectRequest.builder()
        .bucket(bucket).key(key).build(),
      RequestBody.fromBytes(data));
  }

  public byte[] download(String client, String date, String filename) {
    try {
      String key = client + "/" + date + "/" + filename;
      return s3.getObject(GetObjectRequest.builder().bucket(bucket).key(key).build())
               .readAllBytes();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  public void delete(String client, String date, String filename) {
    String key = client + "/" + date + "/" + filename;
    s3.deleteObject(DeleteObjectRequest.builder().bucket(bucket).key(key).build());
    // opcional: borrar de EFS
  }

  public List<String> list(String client) {
    ListObjectsV2Response resp = s3.listObjectsV2(
      ListObjectsV2Request.builder()
        .bucket(bucket)
        .prefix(client + "/")
        .build());
    return resp.contents().stream()
      .map(obj -> obj.key())
      .collect(Collectors.toList());
  }

  public void uploadFile(String key, MultipartFile file) {
    try {
      s3.putObject(
          PutObjectRequest.builder()
              .bucket(bucket)
              .key(key)
              .build(),
          RequestBody.fromBytes(file.getBytes())
      );
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  public byte[] downloadFile(String key) {
    try {
      return s3.getObject(
          GetObjectRequest.builder()
              .bucket(bucket)
              .key(key)
              .build()
      ).readAllBytes();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }
}
