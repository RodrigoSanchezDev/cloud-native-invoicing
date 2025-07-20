package com.sanchezdev.invoiceservice.dto;

import java.time.LocalDate;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

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
