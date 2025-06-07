package com.sanchezdev.invoiceservice.service;

import com.sanchezdev.invoiceservice.model.Invoice;
import com.sanchezdev.invoiceservice.repository.InvoiceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service @RequiredArgsConstructor
public class InvoiceService {
  private final InvoiceRepository repo;
  private final WebClient webClient = WebClient.create();

  private String fileServiceUrl() {
    return System.getProperty("file.service.url", "http://localhost:8081");
  }

  public Invoice createAndUpload(String clientId, LocalDate date, byte[] content, String filename) {
    // 1) llamar file-service para guardar en EFS + S3
    webClient.post()
      .uri(fileServiceUrl() + "/invoices/{client}/{date}", clientId, date.toString())
      .body(Mono.just(content), byte[].class)
      .retrieve()
      .toBodilessEntity()
      .block();

    // 2) guardar metadata en H2
    Invoice inv = new Invoice();
    inv.setClientId(clientId);
    inv.setDate(date);
    inv.setFileName(filename);
    inv.setS3Key(clientId + "/" + date + "/" + filename);
    return repo.save(inv);
  }

  public List<Invoice> listByClient(String clientId) {
    return repo.findByClientId(clientId);
  }

  public List<Invoice> getAllInvoices() {
    return repo.findAll();
  }

  public Optional<Invoice> getInvoiceById(Long id) {
    return repo.findById(id);
  }

  public Invoice saveInvoice(Invoice invoice) {
    return repo.save(invoice);
  }

  public void deleteInvoice(Long id) {
    repo.deleteById(id);
  }

  public byte[] downloadFileFromFileService(String fileServiceUrl, String key) {
    // Llama al file-service para descargar el archivo
    return webClient.get()
      .uri(fileServiceUrl + "/files/download/" + key)
      .retrieve()
      .bodyToMono(byte[].class)
      .block();
  }
}
