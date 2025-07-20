package com.sanchezdev.invoiceservice.controller;

import java.time.LocalDate;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.sanchezdev.invoiceservice.model.Invoice;
import com.sanchezdev.invoiceservice.service.InvoiceService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/invoices")
@RequiredArgsConstructor
public class InvoiceController {
  private final InvoiceService svc;
  @Value("${file.service.url}")
  private String fileServiceUrl;

  @PostMapping("/{clientId}")
  public ResponseEntity<Invoice> create(
      @PathVariable String clientId,
      @RequestParam MultipartFile file,
      @RequestParam String date,
      @AuthenticationPrincipal Jwt jwt) throws Exception {
    
    String roles = jwt.getClaimAsString("extension_Roles");
    System.out.println("Create invoice endpoint called, roles: " + roles);
    
    Invoice inv = svc.createAndUpload(
      clientId,
      LocalDate.parse(date),
      file.getBytes(),
      file.getOriginalFilename()
    );
    return new ResponseEntity<>(inv, HttpStatus.CREATED);
  }

  @GetMapping("/history/{clientId}")
  public ResponseEntity<List<Invoice>> history(@PathVariable String clientId, @AuthenticationPrincipal Jwt jwt) {
    String roles = jwt.getClaimAsString("extension_Roles");
    System.out.println("Get invoice history endpoint called, roles: " + roles);
    
    List<Invoice> invoices = svc.listByClient(clientId);
    return ResponseEntity.ok(invoices);
  }

  @GetMapping
  public ResponseEntity<List<Invoice>> getAllInvoices(@AuthenticationPrincipal Jwt jwt) {
    String roles = jwt.getClaimAsString("extension_Roles");
    System.out.println("Get all invoices endpoint called, roles: " + roles);
    
    List<Invoice> invoices = svc.getAllInvoices();
    return ResponseEntity.ok(invoices);
  }

  @GetMapping("/{id}")
  public ResponseEntity<Invoice> getInvoiceById(@PathVariable Long id, @AuthenticationPrincipal Jwt jwt) {
    String roles = jwt.getClaimAsString("extension_Roles");
    System.out.println("Get invoice by ID endpoint called, roles: " + roles);
    
    return svc.getInvoiceById(id)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
  }

  // ========== TEST ENDPOINTS (No Authentication Required) ==========
  
  @PostMapping("/test/create-with-pdf")
  public ResponseEntity<Invoice> testCreateInvoiceWithPDF(@RequestBody Invoice invoice) {
    System.out.println("TEST: Create invoice with PDF endpoint called (no JWT required)");
    
    Invoice createdInvoice = svc.saveInvoiceWithPDFAndRabbitMQ(invoice);
    return ResponseEntity.ok(createdInvoice);
  }

  @PostMapping("/test/create")
  public ResponseEntity<Invoice> testCreateInvoice(@RequestBody Invoice invoice) {
    System.out.println("TEST: Create invoice endpoint called (no JWT required)");
    
    Invoice createdInvoice = svc.saveInvoiceWithPDFAndRabbitMQ(invoice);
    return ResponseEntity.ok(createdInvoice);
  }

  @GetMapping("/test/list")
  public ResponseEntity<List<Invoice>> testGetAllInvoices() {
    System.out.println("TEST: Get all invoices endpoint called (no JWT required)");
    
    List<Invoice> invoices = svc.getAllInvoices();
    return ResponseEntity.ok(invoices);
  }

  // ========== PRODUCTION ENDPOINTS (JWT Required) ==========

  @PostMapping("/create-with-pdf")
  public ResponseEntity<Invoice> createInvoiceWithPDF(@RequestBody Invoice invoice, @AuthenticationPrincipal Jwt jwt) {
    String roles = jwt.getClaimAsString("extension_Roles");
    System.out.println("Create invoice with PDF endpoint called, roles: " + roles);
    
    Invoice createdInvoice = svc.saveInvoiceWithPDFAndRabbitMQ(invoice);
    return ResponseEntity.ok(createdInvoice);
  }

  @DeleteMapping("/{id}")
  public ResponseEntity<Void> deleteInvoice(@PathVariable Long id, @AuthenticationPrincipal Jwt jwt) {
    String roles = jwt.getClaimAsString("extension_Roles");
    System.out.println("Delete invoice endpoint called, roles: " + roles);
    
    svc.deleteInvoice(id);
    return ResponseEntity.noContent().build();
  }

  @PutMapping("/{id}")
  public ResponseEntity<Invoice> updateInvoice(@PathVariable Long id, @RequestBody Invoice invoice, @AuthenticationPrincipal Jwt jwt) {
    String roles = jwt.getClaimAsString("extension_Roles");
    System.out.println("Update invoice endpoint called, roles: " + roles);
    
    return svc.getInvoiceById(id)
        .map(existing -> {
          invoice.setId(id);
          return ResponseEntity.ok(svc.saveInvoice(invoice));
        })
        .orElse(ResponseEntity.notFound().build());
  }

  @GetMapping("/download/{id}")
  public ResponseEntity<byte[]> downloadInvoice(@PathVariable Long id, @AuthenticationPrincipal Jwt jwt) {
    String roles = jwt.getClaimAsString("extension_Roles");
    System.out.println("Download invoice endpoint called, roles: " + roles);
    
    return svc.getInvoiceById(id)
        .map(inv -> {
           String key = inv.getS3Key(); // Usar s3Key en lugar de fileName
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
