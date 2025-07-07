package com.sanchezdev.rabbitmqservice.controller;

import com.sanchezdev.rabbitmqservice.dto.InvoiceMessageDTO;
import com.sanchezdev.rabbitmqservice.model.BoletaOracle;
import com.sanchezdev.rabbitmqservice.service.InvoiceConsumerService;
import com.sanchezdev.rabbitmqservice.service.RabbitMQProducerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/rabbitmq")
@RequiredArgsConstructor
@Slf4j
public class RabbitMQController {

    private final RabbitMQProducerService producerService;
    private final InvoiceConsumerService consumerService;

    /**
     * Endpoint para enviar mensajes a la cola RabbitMQ
     * Cumple con el requisito: "Desarrolla un endpoint para enviar mensajes a la cola"
     */
    @PostMapping("/send-message")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> sendMessageToQueue(@RequestBody InvoiceMessageDTO message) {
        try {
            log.info("Received request to send message to queue: {}", message);
            producerService.sendInvoiceMessage(message);
            
            return ResponseEntity.ok()
                .body("Message sent to queue successfully. Invoice ID: " + message.getInvoiceId());
        } catch (Exception e) {
            log.error("Error sending message to queue: {}", e.getMessage());
            return ResponseEntity.internalServerError()
                .body("Error sending message: " + e.getMessage());
        }
    }

    /**
     * Endpoint para listar todas las boletas procesadas desde Oracle Cloud
     * Cumple con el requisito: "guardarlos en una base de datos Oracle Cloud"
     */
    @GetMapping("/boletas")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<BoletaOracle>> getAllBoletas() {
        try {
            List<BoletaOracle> boletas = consumerService.getAllBoletas();
            return ResponseEntity.ok(boletas);
        } catch (Exception e) {
            log.error("Error retrieving boletas from Oracle: {}", e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Endpoint para obtener boletas por cliente
     */
    @GetMapping("/boletas/client/{clientId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<BoletaOracle>> getBoletasByClient(@PathVariable String clientId) {
        try {
            List<BoletaOracle> boletas = consumerService.getBoletasByClient(clientId);
            return ResponseEntity.ok(boletas);
        } catch (Exception e) {
            log.error("Error retrieving boletas for client {}: {}", clientId, e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Endpoint para obtener boletas por ID de factura original
     */
    @GetMapping("/boletas/invoice/{invoiceId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<BoletaOracle>> getBoletasByInvoiceId(@PathVariable Long invoiceId) {
        try {
            List<BoletaOracle> boletas = consumerService.getBoletasByOriginalInvoiceId(invoiceId);
            return ResponseEntity.ok(boletas);
        } catch (Exception e) {
            log.error("Error retrieving boletas for invoice ID {}: {}", invoiceId, e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Health check endpoint
     */
    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("RabbitMQ Service is running");
    }
}
