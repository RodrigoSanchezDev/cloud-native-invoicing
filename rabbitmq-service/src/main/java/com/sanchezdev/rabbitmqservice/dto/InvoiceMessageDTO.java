package com.sanchezdev.rabbitmqservice.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class InvoiceMessageDTO {
    private Long invoiceId;
    private String clientId;
    private LocalDate invoiceDate;
    private String fileName;
    private String s3Key;
    private String description;
    private Double amount;
    private String status;
}
