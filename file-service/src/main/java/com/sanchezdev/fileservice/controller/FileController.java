package com.sanchezdev.fileservice.controller;

import com.sanchezdev.fileservice.service.FileStorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import jakarta.servlet.http.HttpServletRequest;
import java.util.List;

@RestController
@RequestMapping("/api/files")
@RequiredArgsConstructor
public class FileController {
  private final FileStorageService fileStorageService;

  @PostMapping("/upload/{client}/{date}")
  public ResponseEntity<String> upload(
      @PathVariable String client,
      @PathVariable String date,
      @RequestParam("file") MultipartFile file,
      @AuthenticationPrincipal Jwt jwt) {
    
    String roles = jwt.getClaimAsString("extension_Roles");
    System.out.println("File upload endpoint called, roles: " + roles);
    
    String key = fileStorageService.saveFile(client, date, file);
    return ResponseEntity.ok(key);
  }

  @GetMapping("/download/**")
  public ResponseEntity<Resource> downloadFile(HttpServletRequest request, @AuthenticationPrincipal Jwt jwt) {
    String roles = jwt.getClaimAsString("extension_Roles");
    System.out.println("File download endpoint called, roles: " + roles);
    
    String key = extractKeyFromPath(request, "/api/files/download/");
    System.out.println("Extracted key: " + key);
    
    byte[] fileBytes = fileStorageService.downloadFile(key);
    Resource resource = new ByteArrayResource(fileBytes);
    return ResponseEntity.ok()
        .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + key)
        .contentType(MediaType.APPLICATION_OCTET_STREAM)
        .body(resource);
  }

  @DeleteMapping("/delete/**")
  public ResponseEntity<Void> deleteFile(HttpServletRequest request, @AuthenticationPrincipal Jwt jwt) {
    String roles = jwt.getClaimAsString("extension_Roles");
    System.out.println("File delete endpoint called, roles: " + roles);
    
    String key = extractKeyFromPath(request, "/api/files/delete/");
    System.out.println("Extracted key for deletion: " + key);
    
    fileStorageService.deleteFile(key);
    return ResponseEntity.noContent().build();
  }
  
  private String extractKeyFromPath(HttpServletRequest request, String prefix) {
    String requestPath = request.getRequestURI();
    return requestPath.substring(prefix.length());
  }

  @GetMapping("/list")
  public ResponseEntity<List<String>> listFiles(@AuthenticationPrincipal Jwt jwt) {
    String roles = jwt.getClaimAsString("extension_Roles");
    System.out.println("File list endpoint called, roles: " + roles);
    
    List<String> files = fileStorageService.listFiles();
    return ResponseEntity.ok(files);
  }

  // ========== INTERNAL ENDPOINTS (No Authentication Required) ==========
  
  @PostMapping("/internal/upload/{client}/{date}")
  public ResponseEntity<String> internalUpload(
      @PathVariable String client,
      @PathVariable String date,
      @RequestParam("file") MultipartFile file) {
    
    System.out.println("Internal file upload endpoint called for client: " + client + ", date: " + date);
    
    try {
      String key = fileStorageService.saveFile(client, date, file);
      System.out.println("File saved successfully with key: " + key);
      return ResponseEntity.ok(key);
    } catch (Exception e) {
      System.err.println("Error saving file: " + e.getMessage());
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
          .body("Error saving file: " + e.getMessage());
    }
  }

  @PostMapping("/internal/upload-pdf")
  public ResponseEntity<String> internalUploadPdf(
      @RequestParam("fileName") String fileName,
      @RequestParam("content") byte[] content) {
    
    System.out.println("Internal PDF upload endpoint called for file: " + fileName);
    
    try {
      String key = fileStorageService.savePdfFile(fileName, content);
      System.out.println("PDF file saved successfully with key: " + key);
      return ResponseEntity.ok(key);
    } catch (Exception e) {
      System.err.println("Error saving PDF file: " + e.getMessage());
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
          .body("Error saving PDF file: " + e.getMessage());
    }
  }

  @GetMapping("/internal/download/**")
  public ResponseEntity<Resource> internalDownloadFile(HttpServletRequest request) {
    System.out.println("Internal file download endpoint called");
    
    String key = extractKeyFromPath(request, "/api/files/internal/download/");
    System.out.println("Extracted key: " + key);
    
    try {
      byte[] fileBytes = fileStorageService.downloadFile(key);
      Resource resource = new ByteArrayResource(fileBytes);
      return ResponseEntity.ok()
          .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + key)
          .contentType(MediaType.APPLICATION_OCTET_STREAM)
          .body(resource);
    } catch (Exception e) {
      System.err.println("Error downloading file: " + e.getMessage());
      return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
    }
  }

  @GetMapping("/internal/list")
  public ResponseEntity<List<String>> internalListFiles() {
    System.out.println("Internal file list endpoint called");
    
    try {
      List<String> files = fileStorageService.listFiles();
      return ResponseEntity.ok(files);
    } catch (Exception e) {
      System.err.println("Error listing files: " + e.getMessage());
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
    }
  }
}
