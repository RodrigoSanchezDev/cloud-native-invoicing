package com.sanchezdev.rabbitmqservice.model;

import java.time.LocalDate;
import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "boletas_audit")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class BoletaAudit {
    
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
    
    // Campos adicionales para auditoría
    @Column(name = "audit_timestamp", nullable = false)
    private LocalDateTime auditTimestamp;
    
    @Column(name = "action", nullable = false)
    private String action; // CREATE, UPDATE, DELETE
    
    @PrePersist
    public void prePersist() {
        if (processedDate == null) {
            processedDate = LocalDateTime.now();
        }
        if (auditTimestamp == null) {
            auditTimestamp = LocalDateTime.now();
        }
        if (status == null) {
            status = "PROCESSED";
        }
        if (action == null) {
            action = "CREATE";
        }
    }
}
