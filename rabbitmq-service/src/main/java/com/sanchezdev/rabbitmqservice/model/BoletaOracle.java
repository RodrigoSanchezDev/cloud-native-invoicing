package com.sanchezdev.rabbitmqservice.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "boletas_oracle")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class BoletaOracle {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "client_id", nullable = false)
    private String clientId;
    
    @Column(name = "invoice_date", nullable = false)
    private LocalDate invoiceDate;
    
    @Column(name = "file_name")
    private String fileName;
    
    @Column(name = "s3_key")
    private String s3Key;
    
    @Column(name = "original_invoice_id")
    private Long originalInvoiceId;
    
    @Column(name = "processed_date", nullable = false)
    private LocalDateTime processedDate;
    
    @Column(name = "status")
    private String status;
    
    @Column(name = "description", length = 1000)
    private String description;
    
    @PrePersist
    public void prePersist() {
        if (processedDate == null) {
            processedDate = LocalDateTime.now();
        }
        if (status == null) {
            status = "PROCESSED";
        }
    }
}
