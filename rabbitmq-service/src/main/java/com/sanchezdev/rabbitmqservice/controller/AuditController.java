package com.sanchezdev.rabbitmqservice.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.sanchezdev.rabbitmqservice.model.BoletaAudit;
import com.sanchezdev.rabbitmqservice.repository.BoletaAuditRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/boletas")
@RequiredArgsConstructor
@Slf4j
public class AuditController {

    private final BoletaAuditRepository boletaAuditRepository;

    /**
     * Endpoint básico para consultar todas las auditorías
     */
    @GetMapping("/audit")
    public ResponseEntity<List<BoletaAudit>> getAllAuditBoletas() {
        log.info("Getting all audit boletas");
        List<BoletaAudit> auditBoletas = boletaAuditRepository.findAllOrderByAuditTimestampDesc();
        return ResponseEntity.ok(auditBoletas);
    }
}
