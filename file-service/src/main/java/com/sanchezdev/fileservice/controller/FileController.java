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

import java.util.List;

@RestController
@RequestMapping("/api/files")
@RequiredArgsConstructor
public class FileController {
  private final FileStorageService fileStorageService;

  @PostMapping("/upload/{client}/{date}")
  public ResponseEntity<Void> upload(
      @PathVariable String client,
      @PathVariable String date,
      @RequestParam("file") MultipartFile file,
      @AuthenticationPrincipal Jwt jwt) {
    
    String roles = jwt.getClaimAsString("extension_Roles");
    System.out.println("File upload endpoint called, roles: " + roles);
    
    fileStorageService.saveFile(client, date, file);
    return ResponseEntity.ok().build();
  }

  @GetMapping("/download")
  public ResponseEntity<Resource> downloadFile(@RequestParam("key") String key, @AuthenticationPrincipal Jwt jwt) {
    String roles = jwt.getClaimAsString("extension_Roles");
    System.out.println("File download endpoint called, roles: " + roles);
    
    byte[] fileBytes = fileStorageService.downloadFile(key);
    Resource resource = new ByteArrayResource(fileBytes);
    return ResponseEntity.ok()
        .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + key)
        .contentType(MediaType.APPLICATION_OCTET_STREAM)
        .body(resource);
  }

  @DeleteMapping("/delete")
  public ResponseEntity<Void> deleteFile(@RequestParam("key") String key, @AuthenticationPrincipal Jwt jwt) {
    String roles = jwt.getClaimAsString("extension_Roles");
    System.out.println("File delete endpoint called, roles: " + roles);
    
    fileStorageService.deleteFile(key);
    return ResponseEntity.noContent().build();
  }

  @GetMapping("/list")
  public ResponseEntity<List<String>> listFiles(@AuthenticationPrincipal Jwt jwt) {
    String roles = jwt.getClaimAsString("extension_Roles");
    System.out.println("File list endpoint called, roles: " + roles);
    
    List<String> files = fileStorageService.listFiles();
    return ResponseEntity.ok(files);
  }
}
