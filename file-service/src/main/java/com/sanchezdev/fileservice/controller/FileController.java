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
  private final FileStorageService svc;

  @PostMapping("/upload")
  public ResponseEntity<String> uploadFile(@RequestParam("file") MultipartFile file) {
    try {
      svc.save(
          file.getOriginalFilename(),
          file.getBytes());
      return ResponseEntity.ok("Archivo subido correctamente");
    } catch (Exception e) {
      return ResponseEntity.status(500).body("Error al subir archivo: " + e.getMessage());
    }
  }

  @GetMapping("/download/{filename}")
  public ResponseEntity<byte[]> downloadFile(@PathVariable String filename) {
    try {
      byte[] data = svc.download(filename);
      return ResponseEntity.ok()
          .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
          .contentType(MediaType.APPLICATION_OCTET_STREAM)
          .body(data);
    } catch (Exception e) {
      return ResponseEntity.status(404).body(null);
    }
  }

  @DeleteMapping("/{client}/{date}/{filename}")
  public ResponseEntity<Void> delete(
      @PathVariable String client,
      @PathVariable String date,
      @PathVariable String filename) {
    svc.delete(client, date, filename);
    return ResponseEntity.noContent().build();
  }

  @GetMapping("/history/{client}")
  public ResponseEntity<List<String>> history(@PathVariable String client) {
    return ResponseEntity.ok(svc.list(client));
  }
}
