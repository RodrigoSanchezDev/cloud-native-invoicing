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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
     * Endpoint para consumir mensajes manualmente de la cola RabbitMQ
     * Cumple con el requisito: "Desarrolla un endpoint para consumir mensajes de la cola"
     */
    @PostMapping("/consume-messages")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> consumeMessagesFromQueue() {
        try {
            log.info("Manual message consumption requested");
            int messagesProcessed = consumerService.consumeMessages();
            
            return ResponseEntity.ok()
                .body("Messages consumed successfully. Processed: " + messagesProcessed);
        } catch (Exception e) {
            log.error("Error consuming messages from queue: {}", e.getMessage());
            return ResponseEntity.internalServerError()
                .body("Error consuming messages: " + e.getMessage());
        }
    }

    /**
     * Health check endpoint
     */
    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("RabbitMQ Service is running");
    }

    /**
     * Public endpoint for testing - send simple message (sin autenticación)
     */
    @PostMapping("/send-test-message")
    public ResponseEntity<?> sendTestMessage() {
        try {
            InvoiceMessageDTO testMessage = new InvoiceMessageDTO();
            testMessage.setInvoiceId(999L);
            testMessage.setClientId("TEST-CLIENT");
            testMessage.setDescription("Test message from rabbitmq-service");
            testMessage.setFileName("test-invoice.pdf");
            testMessage.setS3Key("test/test-invoice.pdf");
            testMessage.setInvoiceDate(java.time.LocalDate.now());

            log.info("Sending test message to queue: {}", testMessage);
            producerService.sendInvoiceMessage(testMessage);
            
            return ResponseEntity.ok()
                .body("Test message sent successfully! Invoice ID: " + testMessage.getInvoiceId());
        } catch (Exception e) {
            log.error("Error sending test message: {}", e.getMessage());
            return ResponseEntity.internalServerError()
                .body("Error sending test message: " + e.getMessage());
        }
    }

    /**
     * Public endpoint for testing - consume messages (sin autenticación)
     */
    @PostMapping("/consume-test")
    public ResponseEntity<?> consumeTestMessages() {
        try {
            log.info("Manual test message consumption requested");
            int messagesProcessed = consumerService.consumeMessages();
            
            return ResponseEntity.ok()
                .body("Test consumption completed. Messages processed: " + messagesProcessed);
        } catch (Exception e) {
            log.error("Error in test consumption: {}", e.getMessage());
            return ResponseEntity.internalServerError()
                .body("Error in test consumption: " + e.getMessage());
        }
    }

    /**
     * Public endpoint - get all boletas (sin autenticación para pruebas)
     */
    @GetMapping("/boletas-test")
    public ResponseEntity<List<BoletaOracle>> getAllBoletasTest() {
        try {
            List<BoletaOracle> boletas = consumerService.getAllBoletas();
            return ResponseEntity.ok(boletas);
        } catch (Exception e) {
            log.error("Error retrieving boletas: {}", e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Endpoint para mostrar la información de la cola incluyendo cantidad de mensajes
     */
    @GetMapping("/queue-info")
    public ResponseEntity<?> getQueueInfo() {
        try {
            log.info("Getting queue information");
            
            // Obtener información de la cola usando RabbitMQ Management API
            var queueInfo = consumerService.getQueueInfo();
            
            return ResponseEntity.ok(queueInfo);
        } catch (Exception e) {
            log.error("Error getting queue info: {}", e.getMessage());
            return ResponseEntity.internalServerError()
                .body("Error getting queue information: " + e.getMessage());
        }
    }

    /**
     * Endpoint para mostrar la información de la cola sin autenticación (para pruebas)
     */
    @GetMapping("/queue-info-test")
    public ResponseEntity<?> getQueueInfoTest() {
        try {
            log.info("Getting queue information (test endpoint)");
            
            // Obtener información de la cola usando RabbitMQ Management API
            var queueInfo = consumerService.getQueueInfo();
            
            return ResponseEntity.ok(queueInfo);
        } catch (Exception e) {
            log.error("Error getting queue info: {}", e.getMessage());
            return ResponseEntity.internalServerError()
                .body("Error getting queue information: " + e.getMessage());
        }
    }

    // 🆕 ENDPOINTS PARA MANEJO DE DLQ (DEAD LETTER QUEUE)
    
    /**
     * Endpoint para enviar mensaje con error a DLQ - PARA TESTING
     */
    @PostMapping("/send-error-message")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> sendErrorMessageToDLQ(@RequestBody InvoiceMessageDTO message) {
        try {
            log.info("Received request to send ERROR message: {}", message);
            
            // El producer service determinará automáticamente si va a DLQ
            producerService.sendInvoiceMessage(message);
            
            return ResponseEntity.ok()
                .body("Error message sent to queue (will be routed to DLQ). Invoice ID: " + message.getInvoiceId());
        } catch (Exception e) {
            log.error("Error sending error message: {}", e.getMessage());
            return ResponseEntity.internalServerError()
                .body("Error sending error message: " + e.getMessage());
        }
    }
    
    /**
     * Endpoint para consultar información de la DLQ
     */
    @GetMapping("/dlq-info")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> getDLQInfo() {
        try {
            var dlqInfo = consumerService.getDLQInfo();
            var normalQueueInfo = consumerService.getQueueInfo();
            
            Map<String, Object> result = new HashMap<>();
            result.put("normalQueue", Map.of(
                "name", normalQueueInfo.name,
                "messageCount", normalQueueInfo.messageCount,
                "consumerCount", normalQueueInfo.consumerCount
            ));
            result.put("dlqQueue", Map.of(
                "name", dlqInfo.name,
                "messageCount", dlqInfo.messageCount,
                "consumerCount", dlqInfo.consumerCount
            ));
            result.put("timestamp", java.time.LocalDateTime.now());
            
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("Error getting DLQ info: {}", e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * Endpoint para listar mensajes en DLQ (para debugging)
     */
    @PostMapping("/consume-dlq-messages")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> consumeDLQMessages() {
        try {
            log.info("Manual DLQ message inspection requested");
            int dlqMessagesFound = consumerService.getDLQMessages();
            
            return ResponseEntity.ok()
                .body("DLQ inspection completed. Error messages found: " + dlqMessagesFound);
        } catch (Exception e) {
            log.error("Error inspecting DLQ: {}", e.getMessage());
            return ResponseEntity.internalServerError()
                .body("Error inspecting DLQ: " + e.getMessage());
        }
    }
}

/**
 * Controller adicional para endpoints simples de prueba
 */
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Slf4j
class SimpleRabbitMQController {
    
    private final InvoiceConsumerService consumerService;
    
    /**
     * Endpoint simple para consumir mensajes manualmente - Diseñado para pruebas
     * URL: POST /api/consume
     */
    @PostMapping("/consume")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> consumeMessages() {
        try {
            log.info("Manual message consumption requested via simple endpoint");
            int messagesProcessed = consumerService.consumeMessages();
            
            return ResponseEntity.ok("Messages consumed: " + messagesProcessed);
        } catch (Exception e) {
            log.error("Error consuming messages: {}", e.getMessage());
            return ResponseEntity.internalServerError()
                .body("Error: " + e.getMessage());
        }
    }
    
    /**
     * Endpoint simple para listar boletas guardadas en H2
     * URL: GET /api/boletas
     */
    @GetMapping("/boletas")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<BoletaOracle>> getBoletas() {
        try {
            log.info("Getting all boletas from H2");
            List<BoletaOracle> boletas = consumerService.getAllBoletas();
            return ResponseEntity.ok(boletas);
        } catch (Exception e) {
            log.error("Error getting boletas: {}", e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * Endpoint simple para información de la cola
     * URL: GET /api/queue-info
     */
    @GetMapping("/queue-info")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> getQueueInfo() {
        try {
            var queueInfo = consumerService.getQueueInfo();
            return ResponseEntity.ok(queueInfo);
        } catch (Exception e) {
            log.error("Error getting queue info: {}", e.getMessage());
            return ResponseEntity.internalServerError()
                .body("Error: " + e.getMessage());
        }
    }
}
