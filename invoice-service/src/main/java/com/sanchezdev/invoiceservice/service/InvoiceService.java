package com.sanchezdev.invoiceservice.service;

import com.sanchezdev.invoiceservice.dto.InvoiceMessageDTO;
import com.sanchezdev.invoiceservice.model.Invoice;
import com.sanchezdev.invoiceservice.repository.InvoiceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
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
  
  @Autowired(required = false)
  private RabbitMQProducerService rabbitMQProducerService;

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
        fileServiceUrl + "/api/files/internal/upload/" + clientId + "/" + date.toString(),
        entity, String.class
    );

    // 2) Save metadata on H2
    Invoice inv = new Invoice();
    inv.setClientId(clientId);
    inv.setDate(date);
    inv.setFileName(filename);
    inv.setS3Key(clientId + "/" + date + "/" + filename);
    Invoice savedInvoice = repo.save(inv);
    
    // 3) Send message to RabbitMQ queue (if enabled)
    if (rabbitMQProducerService != null) {
      try {
        InvoiceMessageDTO message = new InvoiceMessageDTO();
        message.setInvoiceId(savedInvoice.getId());
        message.setClientId(clientId);
        message.setInvoiceDate(date);
        message.setFileName(filename);
        message.setS3Key(savedInvoice.getS3Key());
        message.setDescription("Boleta creada - " + filename);
        message.setStatus("CREATED");
        
        rabbitMQProducerService.sendInvoiceCreatedMessage(message);
      } catch (Exception e) {
        // Log error but don't fail the main process
        System.err.println("Error sending message to RabbitMQ: " + e.getMessage());
      }
    }
    
    return savedInvoice;
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

  public Invoice saveInvoiceWithRabbitMQ(Invoice invoice) {
    // Save to database
    Invoice savedInvoice = repo.save(invoice);
    
    // Send message to RabbitMQ queue (if enabled)
    if (rabbitMQProducerService != null) {
      try {
        InvoiceMessageDTO message = new InvoiceMessageDTO();
        message.setInvoiceId(savedInvoice.getId());
        message.setClientId(savedInvoice.getClientId());
        message.setInvoiceDate(savedInvoice.getDate());
        message.setFileName(savedInvoice.getFileName());
        message.setS3Key(savedInvoice.getS3Key());
        message.setDescription(savedInvoice.getDescription() != null ? savedInvoice.getDescription() : "Factura creada");
        message.setAmount(savedInvoice.getAmount());
        message.setStatus("CREATED");
        
        System.out.println("Sending message to RabbitMQ for invoice ID: " + savedInvoice.getId());
        rabbitMQProducerService.sendInvoiceCreatedMessage(message);
        System.out.println("Message sent successfully to RabbitMQ");
      } catch (Exception e) {
        // Log error but don't fail the main process
        System.err.println("Error sending message to RabbitMQ: " + e.getMessage());
        e.printStackTrace();
      }
    } else {
      System.out.println("RabbitMQ Producer Service is not available");
    }
    
    return savedInvoice;
  }

  public void deleteInvoice(Long id) {
    repo.deleteById(id);
  }

  public Invoice saveInvoiceWithPDFAndRabbitMQ(Invoice invoice) {
    // 1) Save to database first
    Invoice savedInvoice = repo.save(invoice);
    
    // 2) Generate PDF content automatically
    try {
      String pdfContent = generateInvoicePDF(savedInvoice);
      byte[] pdfBytes = pdfContent.getBytes("UTF-8");
      String filename = "invoice-" + savedInvoice.getId() + ".pdf";
      
      // 3) Upload PDF to S3 via file-service
      HttpHeaders headers = new HttpHeaders();
      headers.setContentType(MediaType.MULTIPART_FORM_DATA);
      LinkedMultiValueMap<String, Object> form = new LinkedMultiValueMap<>();
      form.add("file", new ByteArrayResource(pdfBytes) {
          @Override public String getFilename() { return filename; }
      });
      HttpEntity<MultiValueMap<String, Object>> entity = new HttpEntity<>(form, headers);
      
      String uploadUrl = fileServiceUrl + "/api/files/internal/upload/" + savedInvoice.getClientId() + "/" + 
                        (savedInvoice.getDate() != null ? savedInvoice.getDate() : LocalDate.now());
      restTemplate.postForEntity(uploadUrl, entity, String.class);
      
      // 4) Update invoice with file information
      savedInvoice.setFileName(filename);
      savedInvoice.setS3Key(savedInvoice.getClientId() + "/" + 
                           (savedInvoice.getDate() != null ? savedInvoice.getDate() : LocalDate.now()) + 
                           "/" + filename);
      if (savedInvoice.getDate() == null) {
        savedInvoice.setDate(LocalDate.now());
      }
      savedInvoice = repo.save(savedInvoice);
      
    } catch (Exception e) {
      System.err.println("Error generating/uploading PDF: " + e.getMessage());
      // Continue without PDF if there's an error
    }
    
    // 5) Send message to RabbitMQ queue (if enabled)
    if (rabbitMQProducerService != null) {
      try {
        InvoiceMessageDTO message = new InvoiceMessageDTO();
        message.setInvoiceId(savedInvoice.getId());
        message.setClientId(savedInvoice.getClientId());
        message.setInvoiceDate(savedInvoice.getDate());
        message.setFileName(savedInvoice.getFileName());
        message.setS3Key(savedInvoice.getS3Key());
        message.setDescription(savedInvoice.getDescription() != null ? savedInvoice.getDescription() : "Factura creada con PDF automático");
        message.setAmount(savedInvoice.getAmount());
        message.setStatus("CREATED");
        
        System.out.println("Sending message to RabbitMQ for invoice ID: " + savedInvoice.getId());
        rabbitMQProducerService.sendInvoiceCreatedMessage(message);
        System.out.println("Message sent successfully to RabbitMQ");
      } catch (Exception e) {
        System.err.println("Error sending message to RabbitMQ: " + e.getMessage());
        e.printStackTrace();
      }
    } else {
      System.out.println("RabbitMQ Producer Service is not available");
    }
    
    return savedInvoice;
  }

  private String generateInvoicePDF(Invoice invoice) {
    // Generate a simple PDF content (could be enhanced with actual PDF library)
    StringBuilder pdfContent = new StringBuilder();
    pdfContent.append("FACTURA/BOLETA\n");
    pdfContent.append("================\n\n");
    pdfContent.append("ID: ").append(invoice.getId()).append("\n");
    pdfContent.append("Cliente: ").append(invoice.getClientId()).append("\n");
    pdfContent.append("Fecha: ").append(invoice.getDate() != null ? invoice.getDate() : LocalDate.now()).append("\n");
    pdfContent.append("Descripción: ").append(invoice.getDescription() != null ? invoice.getDescription() : "Sin descripción").append("\n");
    pdfContent.append("Monto: $").append(invoice.getAmount() != null ? invoice.getAmount() : 0.0).append("\n\n");
    pdfContent.append("Generado automáticamente por el sistema de facturación.\n");
    pdfContent.append("Fecha de generación: ").append(LocalDate.now()).append("\n");
    
    return pdfContent.toString();
  }

  public byte[] downloadFileFromFileService(String fileServiceUrl, String key) {
    // Downloading using RestTemplate
    return restTemplate.getForObject(
        fileServiceUrl + "/api/files/internal/download/" + key,
        byte[].class
    );
  }
}
