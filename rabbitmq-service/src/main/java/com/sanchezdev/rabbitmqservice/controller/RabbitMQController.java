package com.sanchezdev.rabbitmqservice.controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.sanchezdev.rabbitmqservice.dto.InvoiceMessageDTO;
import com.sanchezdev.rabbitmqservice.model.BoletaOracle;
import com.sanchezdev.rabbitmqservice.service.InvoiceConsumerService;
import com.sanchezdev.rabbitmqservice.service.RabbitMQProducerService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

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
    
    @Autowired
    private ApplicationContext applicationContext;
    
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
    
    /**
     * Endpoint para verificar el estado del auto-consumer
     * URL: GET /api/auto-consumer-status
     */
    @GetMapping("/auto-consumer-status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> getAutoConsumerStatus() {
        try {
            // Verificar si AutoConsumerService está activo consultando el ApplicationContext
            boolean autoConsumerEnabled = consumerService.isAutoConsumerEnabled();
            
            Map<String, Object> status = new HashMap<>();
            status.put("autoConsumerEnabled", autoConsumerEnabled);
            status.put("timestamp", java.time.LocalDateTime.now());
            status.put("message", autoConsumerEnabled ? 
                "Auto-consumer is ACTIVE - messages are processed automatically" : 
                "Auto-consumer is DISABLED - use manual consumption endpoints");
            
            return ResponseEntity.ok(status);
        } catch (Exception e) {
            log.error("Error getting auto-consumer status: {}", e.getMessage());
            return ResponseEntity.internalServerError()
                .body("Error: " + e.getMessage());
        }
    }
    
    /**
     * Endpoint para activar el auto-consumer dinámicamente
     * URL: POST /api/auto-consumer/enable
     */
    @PostMapping("/auto-consumer/enable")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> enableAutoConsumer() {
        try {
            boolean result = consumerService.setAutoConsumerEnabled(true);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", result);
            response.put("autoConsumerEnabled", true);
            response.put("message", "Auto-consumer enabled successfully");
            response.put("timestamp", java.time.LocalDateTime.now());
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error enabling auto-consumer: {}", e.getMessage());
            return ResponseEntity.internalServerError()
                .body("Error enabling auto-consumer: " + e.getMessage());
        }
    }
    
    /**
     * Endpoint para desactivar el auto-consumer dinámicamente
     * URL: POST /api/auto-consumer/disable
     */
    @PostMapping("/auto-consumer/disable")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> disableAutoConsumer() {
        try {
            boolean result = consumerService.setAutoConsumerEnabled(false);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", result);
            response.put("autoConsumerEnabled", false);
            response.put("message", "Auto-consumer disabled successfully");
            response.put("timestamp", java.time.LocalDateTime.now());
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error disabling auto-consumer: {}", e.getMessage());
            return ResponseEntity.internalServerError()
                .body("Error disabling auto-consumer: " + e.getMessage());
        }
    }
    
    /**
     * Endpoint mejorado para activar el auto-consumer real dinámicamente
     * URL: POST /api/auto-consumer-real/enable
     */
    @PostMapping("/auto-consumer-real/enable")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> enableRealAutoConsumer() {
        try {
            // Intentar obtener AutoConsumerService del contexto
            Optional<Object> autoConsumerService = getAutoConsumerService();
            
            if (autoConsumerService.isPresent()) {
                Object service = autoConsumerService.get();
                // Usar reflection para llamar enableAutoConsumer()
                boolean result = (Boolean) service.getClass()
                    .getMethod("enableAutoConsumer")
                    .invoke(service);
                
                Map<String, Object> response = new HashMap<>();
                response.put("success", result);
                response.put("autoConsumerEnabled", true);
                response.put("message", "REAL Auto-consumer enabled successfully via AutoConsumerService");
                response.put("timestamp", java.time.LocalDateTime.now());
                response.put("service", "AutoConsumerService");
                
                return ResponseEntity.ok(response);
            } else {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("autoConsumerEnabled", false);
                response.put("message", "AutoConsumerService not available - falling back to fake method");
                response.put("timestamp", java.time.LocalDateTime.now());
                response.put("service", "InvoiceConsumerService (fake)");
                
                return ResponseEntity.ok(response);
            }
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("error", e.getMessage());
            response.put("message", "Error enabling real auto-consumer");
            response.put("timestamp", java.time.LocalDateTime.now());
            
            return ResponseEntity.internalServerError().body(response);
        }
    }
    
    /**
     * Endpoint mejorado para desactivar el auto-consumer real dinámicamente
     * URL: POST /api/auto-consumer-real/disable
     */
    @PostMapping("/auto-consumer-real/disable")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> disableRealAutoConsumer() {
        try {
            Optional<Object> autoConsumerService = getAutoConsumerService();
            
            if (autoConsumerService.isPresent()) {
                Object service = autoConsumerService.get();
                boolean result = (Boolean) service.getClass()
                    .getMethod("disableAutoConsumer")
                    .invoke(service);
                
                Map<String, Object> response = new HashMap<>();
                response.put("success", result);
                response.put("autoConsumerEnabled", false);
                response.put("message", "REAL Auto-consumer disabled successfully via AutoConsumerService");
                response.put("timestamp", java.time.LocalDateTime.now());
                response.put("service", "AutoConsumerService");
                
                return ResponseEntity.ok(response);
            } else {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("autoConsumerEnabled", true);
                response.put("message", "AutoConsumerService not available - cannot disable real auto-consumer");
                response.put("timestamp", java.time.LocalDateTime.now());
                response.put("service", "InvoiceConsumerService (fake)");
                
                return ResponseEntity.ok(response);
            }
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("error", e.getMessage());
            response.put("message", "Error disabling real auto-consumer");
            response.put("timestamp", java.time.LocalDateTime.now());
            
            return ResponseEntity.internalServerError().body(response);
        }
    }
    
    /**
     * Endpoint mejorado para verificar el estado real del auto-consumer
     * URL: GET /api/auto-consumer-real/status
     */
    @GetMapping("/auto-consumer-real/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> getRealAutoConsumerStatus() {
        try {
            Optional<Object> autoConsumerService = getAutoConsumerService();
            
            if (autoConsumerService.isPresent()) {
                Object service = autoConsumerService.get();
                boolean isActive = (Boolean) service.getClass()
                    .getMethod("isAutoConsumerActive")
                    .invoke(service);
                
                Map<String, Object> status = new HashMap<>();
                status.put("autoConsumerEnabled", isActive);
                status.put("timestamp", java.time.LocalDateTime.now());
                status.put("message", isActive ? 
                    "REAL Auto-consumer is ACTIVE - messages processed automatically by AutoConsumerService" : 
                    "REAL Auto-consumer is DISABLED - AutoConsumerService rejects messages");
                status.put("service", "AutoConsumerService");
                status.put("available", true);
                
                return ResponseEntity.ok(status);
            } else {
                Map<String, Object> status = new HashMap<>();
                status.put("autoConsumerEnabled", false);
                status.put("timestamp", java.time.LocalDateTime.now());
                status.put("message", "AutoConsumerService not available - check environment variable RABBITMQ_AUTO_CONSUMER_ENABLED");
                status.put("service", "Not available");
                status.put("available", false);
                
                return ResponseEntity.ok(status);
            }
        } catch (Exception e) {
            Map<String, Object> status = new HashMap<>();
            status.put("autoConsumerEnabled", false);
            status.put("error", e.getMessage());
            status.put("message", "Error checking real auto-consumer status");
            status.put("timestamp", java.time.LocalDateTime.now());
            
            return ResponseEntity.internalServerError().body(status);
        }
    }
    
    /**
     * Helper method para obtener AutoConsumerService del contexto Spring
     */
    private Optional<Object> getAutoConsumerService() {
        try {
            Object service = applicationContext.getBean("autoConsumerService");
            return Optional.of(service);
        } catch (Exception e) {
            return Optional.empty();
        }
    }
}
