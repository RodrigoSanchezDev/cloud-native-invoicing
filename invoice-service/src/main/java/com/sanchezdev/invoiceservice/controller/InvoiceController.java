package com.sanchezdev.invoiceservice.controller;

import com.sanchezdev.invoiceservice.model.Invoice;
import com.sanchezdev.invoiceservice.service.InvoiceService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/invoices")
@RequiredArgsConstructor
public class InvoiceController {
  private final InvoiceService svc;
  @Value("${file.service.url}")
  private String fileServiceUrl;

  @PostMapping("/{clientId}")
  @PreAuthorize("hasAuthority('ROLE_InvoiceManager')")
  public ResponseEntity<Invoice> create(
      @PathVariable String clientId,
      @RequestParam MultipartFile file,
      @RequestParam String date) throws Exception {
    Invoice inv = svc.createAndUpload(
      clientId,
      LocalDate.parse(date),
      file.getBytes(),
      file.getOriginalFilename()
    );
    return new ResponseEntity<>(inv, HttpStatus.CREATED);
  }

  @GetMapping("/history/{clientId}")
  @PreAuthorize("hasAuthority('ROLE_InvoiceManager') or hasAuthority('ROLE_InvoiceReader')")
  public List<Invoice> history(@PathVariable String clientId) {
    return svc.listByClient(clientId);
  }

  @GetMapping
  @PreAuthorize("hasAuthority('ROLE_InvoiceManager') or hasAuthority('ROLE_InvoiceReader')")
  public List<Invoice> getAllInvoices() {
    return svc.getAllInvoices();
  }

  @GetMapping("/{id}")
  @PreAuthorize("hasAuthority('ROLE_InvoiceManager') or hasAuthority('ROLE_InvoiceReader')")
  public ResponseEntity<Invoice> getInvoiceById(@PathVariable Long id) {
    return svc.getInvoiceById(id)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
  }

  @PostMapping
  @PreAuthorize("hasAuthority('ROLE_InvoiceManager')")
  public Invoice createInvoice(@RequestBody Invoice invoice) {
    return svc.saveInvoice(invoice);
  }

  @DeleteMapping("/{id}")
  @PreAuthorize("hasAuthority('ROLE_InvoiceManager')")
  public ResponseEntity<Void> deleteInvoice(@PathVariable Long id) {
    svc.deleteInvoice(id);
    return ResponseEntity.noContent().build();
  }

  @PutMapping("/{id}")
  @PreAuthorize("hasAuthority('ROLE_InvoiceManager')")
  public ResponseEntity<Invoice> updateInvoice(@PathVariable Long id, @RequestBody Invoice invoice) {
    return svc.getInvoiceById(id)
        .map(existing -> {
          invoice.setId(id);
          return ResponseEntity.ok(svc.saveInvoice(invoice));
        })
        .orElse(ResponseEntity.notFound().build());
  }

  @GetMapping("/download/{id}")
  @PreAuthorize("hasAuthority('ROLE_InvoiceManager') or hasAuthority('ROLE_InvoiceReader')")
  public ResponseEntity<byte[]> downloadInvoice(@PathVariable Long id) {
    return svc.getInvoiceById(id)
        .map(inv -> {
           String key = inv.getFileName();
           try {
            byte[] file = svc.downloadFileFromFileService(fileServiceUrl, key);
             return ResponseEntity.ok()
                 .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + key + "\"")
                 .contentType(MediaType.APPLICATION_OCTET_STREAM)
                 .body(file);
           } catch (Exception e) {
             e.printStackTrace(); 
             return ResponseEntity.status(500)
                 .body(("Error descargando archivo: " + e.getMessage()).getBytes());
           }
        })
        .orElse(ResponseEntity.status(404).body(null));
  }
}
