package com.sanchezdev.invoiceservice.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
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
