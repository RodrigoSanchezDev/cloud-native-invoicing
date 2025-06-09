package com.sanchezdev.invoiceservice.service;

import com.sanchezdev.invoiceservice.model.Invoice;
import com.sanchezdev.invoiceservice.repository.InvoiceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service @RequiredArgsConstructor
public class InvoiceService {
  private final InvoiceRepository repo;
  private final RestTemplate restTemplate = new RestTemplate();
  @Value("${file.service.url}") private String fileServiceUrl;

  public Invoice createAndUpload(String clientId, LocalDate date, byte[] content, String filename) {
    // 1) Calling file-service to save on EFS + S3 w/ RestTemplate multipart
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.MULTIPART_FORM_DATA);
    LinkedMultiValueMap<String, Object> form = new LinkedMultiValueMap<>();
    form.add("file", new ByteArrayResource(content) {
        @Override public String getFilename() { return filename; }
    });
    HttpEntity<MultiValueMap<String, Object>> entity = new HttpEntity<>(form, headers);
    restTemplate.postForEntity(
        fileServiceUrl + "/files/upload/" + clientId + "/" + date.toString(),
        entity, String.class
    );

    // 2) Save metadata on H2
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
    // Downloading using RestTemplate
    return restTemplate.getForObject(
        fileServiceUrl + "/files/download/" + key,
        byte[].class
    );
  }
}
