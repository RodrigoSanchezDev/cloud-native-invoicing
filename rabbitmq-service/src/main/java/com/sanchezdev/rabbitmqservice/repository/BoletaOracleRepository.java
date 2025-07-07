package com.sanchezdev.rabbitmqservice.repository;

import com.sanchezdev.rabbitmqservice.model.BoletaOracle;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BoletaOracleRepository extends JpaRepository<BoletaOracle, Long> {
    
    List<BoletaOracle> findByClientId(String clientId);
    
    List<BoletaOracle> findByStatus(String status);
    
    @Query("SELECT b FROM BoletaOracle b WHERE b.originalInvoiceId = :invoiceId")
    List<BoletaOracle> findByOriginalInvoiceId(@Param("invoiceId") Long invoiceId);
    
    @Query("SELECT b FROM BoletaOracle b ORDER BY b.processedDate DESC")
    List<BoletaOracle> findAllOrderByProcessedDateDesc();
}
