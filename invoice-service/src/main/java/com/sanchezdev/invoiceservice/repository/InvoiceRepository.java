package com.sanchezdev.invoiceservice.repository;

import com.sanchezdev.invoiceservice.model.Invoice;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InvoiceRepository extends JpaRepository<Invoice, Long> {
}
