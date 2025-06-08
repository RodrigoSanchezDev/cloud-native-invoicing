package com.sanchezdev.fileservice.controller;

import com.sanchezdev.fileservice.service.FileStorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/files")
@RequiredArgsConstructor
public class FileController {
  private final FileStorageService fileStorageService;

  @PostMapping("/upload/{client}/{date}")
  public ResponseEntity<Void> upload(
      @PathVariable String client,
      @PathVariable String date,
      @RequestParam("file") MultipartFile file) {
    fileStorageService.saveFile(client, date, file);
    return ResponseEntity.ok().build();
  }

  @GetMapping("/download/{key}")
  public ResponseEntity<byte[]> download(@PathVariable String key) {
    byte[] fileBytes = fileStorageService.downloadFile(key);
    return ResponseEntity.ok()
        .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + key)
        .contentType(MediaType.APPLICATION_OCTET_STREAM)
        .body(fileBytes);
  }
}
