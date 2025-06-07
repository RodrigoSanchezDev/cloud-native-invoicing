package com.sanchezdev.invoiceservice.service;

import com.sanchezdev.invoiceservice.model.Invoice;
import com.sanchezdev.invoiceservice.repository.InvoiceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service @RequiredArgsConstructor
public class InvoiceService {
  private final InvoiceRepository repo;
  private final WebClient webClient = WebClient.create();
  @Value("${file.service.url}") private String fileServiceUrl;

  public Invoice createAndUpload(String clientId, LocalDate date, byte[] content, String filename) {
    // 1) llamar file-service para guardar en EFS + S3
    MultiValueMap<String, org.springframework.core.io.Resource> formData = new LinkedMultiValueMap<>();
    org.springframework.core.io.ByteArrayResource resource = new org.springframework.core.io.ByteArrayResource(content) {
      @Override
      public String getFilename() { return filename; }
    };
    formData.add("file", resource);
    webClient.post()
      .uri(fileServiceUrl + "/files/upload/{client}/{date}", clientId, date.toString())
      .contentType(org.springframework.http.MediaType.MULTIPART_FORM_DATA)
      .body(org.springframework.web.reactive.function.BodyInserters.fromMultipartData(formData))
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
