package com.sanchezdev.fileservice.controller;

import com.sanchezdev.fileservice.service.FileStorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/files")
@RequiredArgsConstructor
public class FileController {
  private final FileStorageService fileStorageService;

  @PostMapping("/upload/{client}/{date}")
  @PreAuthorize("hasAuthority('ROLE_InvoiceManager')")
  public ResponseEntity<Void> upload(
      @PathVariable String client,
      @PathVariable String date,
      @RequestParam("file") MultipartFile file) {
    fileStorageService.saveFile(client, date, file);
    return ResponseEntity.ok().build();
  }

  @GetMapping("/download/{key:.+}")
  @PreAuthorize("hasAuthority('ROLE_InvoiceManager') or hasAuthority('ROLE_InvoiceReader')")
  public ResponseEntity<byte[]> download(@PathVariable("key") String key) {
    byte[] fileBytes = fileStorageService.downloadFile(key);
    return ResponseEntity.ok()
        .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + key)
        .contentType(MediaType.APPLICATION_OCTET_STREAM)
        .body(fileBytes);
  }

  @DeleteMapping("/delete/{key:.+}")
  @PreAuthorize("hasAuthority('ROLE_InvoiceManager')")
  public ResponseEntity<Void> deleteFile(@PathVariable("key") String key) {
    fileStorageService.deleteFile(key);
    return ResponseEntity.noContent().build();
  }

  @GetMapping("/list")
  @PreAuthorize("hasAuthority('ROLE_InvoiceManager') or hasAuthority('ROLE_InvoiceReader')")
  public ResponseEntity<List<String>> listFiles() {
    List<String> files = fileStorageService.listFiles();
    return ResponseEntity.ok(files);
  }
}
