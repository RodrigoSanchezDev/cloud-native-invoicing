package com.sanchezdev.rabbitmqservice.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.sanchezdev.rabbitmqservice.model.BoletaAudit;

@Repository
public interface BoletaAuditRepository extends JpaRepository<BoletaAudit, Long> {
    
    List<BoletaAudit> findByClientId(String clientId);
    
    List<BoletaAudit> findByStatus(String status);
    
    List<BoletaAudit> findByAction(String action);
    
    @Query("SELECT b FROM BoletaAudit b WHERE b.originalInvoiceId = :invoiceId")
    List<BoletaAudit> findByOriginalInvoiceId(@Param("invoiceId") Long invoiceId);
    
    @Query("SELECT b FROM BoletaAudit b ORDER BY b.auditTimestamp DESC")
    List<BoletaAudit> findAllOrderByAuditTimestampDesc();
}
