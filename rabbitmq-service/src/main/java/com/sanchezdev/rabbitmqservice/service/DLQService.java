package com.sanchezdev.rabbitmqservice.service;

import com.sanchezdev.rabbitmqservice.config.RabbitMQConfig;
import com.sanchezdev.rabbitmqservice.dto.InvoiceMessageDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

/**
 * Servicio para manejo de Dead Letter Queue (DLQ)
 * Procesa mensajes con errores sin afectar el flujo principal
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DLQService {

    private final RabbitTemplate rabbitTemplate;

    /**
     * Envía un mensaje a la cola DLQ cuando hay errores
     */
    public void sendMessageToDLQ(InvoiceMessageDTO message, String errorReason) {
        try {
            log.warn("Sending message to DLQ due to error: {}", errorReason);
            log.info("Message details: {}", message);
            
            // Agregar información del error al mensaje
            InvoiceMessageDTO dlqMessage = new InvoiceMessageDTO();
            dlqMessage.setInvoiceId(message.getInvoiceId());
            dlqMessage.setClientId(message.getClientId());
            dlqMessage.setInvoiceDate(message.getInvoiceDate());
            dlqMessage.setFileName(message.getFileName());
            dlqMessage.setS3Key(message.getS3Key());
            dlqMessage.setDescription("ERROR: " + errorReason + " | Original: " + message.getDescription());
            dlqMessage.setAmount(message.getAmount());
            dlqMessage.setStatus("ERROR");
            
            rabbitTemplate.convertAndSend(
                RabbitMQConfig.INVOICE_DLQ_EXCHANGE,
                RabbitMQConfig.INVOICE_DLQ_ROUTING_KEY,
                dlqMessage
            );
            
            log.info("Message sent to DLQ successfully. Invoice ID: {}", message.getInvoiceId());
        } catch (Exception e) {
            log.error("Error sending message to DLQ: {}", e.getMessage(), e);
        }
    }

    /**
     * Valida si un mensaje tiene errores que deberían enviarlo a DLQ
     */
    public boolean shouldSendToDLQ(InvoiceMessageDTO message) {
        try {
            // Validación 1: Campos obligatorios nulos
            if (message.getInvoiceId() == null) {
                log.error("Invoice ID is null");
                return true;
            }
            
            if (message.getClientId() == null || message.getClientId().trim().isEmpty()) {
                log.error("Client ID is null or empty");
                return true;
            }
            
            // Validación 2: Montos negativos o inválidos  
            if (message.getAmount() != null && message.getAmount() < 0) {
                log.error("Amount is negative: {}", message.getAmount());
                return true;
            }
            
            // Validación 3: Descripción contiene "ERROR" (para simular errores)
            if (message.getDescription() != null && message.getDescription().toUpperCase().contains("ERROR")) {
                log.error("Description contains ERROR keyword: {}", message.getDescription());
                return true;
            }
            
            // Validación 4: Cliente ID con formato de error (para pruebas)
            if (message.getClientId() != null && message.getClientId().toUpperCase().contains("ERROR")) {
                log.error("Client ID contains ERROR keyword: {}", message.getClientId());
                return true;
            }
            
            return false;
        } catch (Exception e) {
            log.error("Error during validation - sending to DLQ: {}", e.getMessage());
            return true;
        }
    }

    /**
     * Obtiene el motivo específico del error para logging
     */
    public String getErrorReason(InvoiceMessageDTO message) {
        if (message.getInvoiceId() == null) {
            return "Invoice ID is null";
        }
        if (message.getClientId() == null || message.getClientId().trim().isEmpty()) {
            return "Client ID is null or empty";
        }
        if (message.getAmount() != null && message.getAmount() < 0) {
            return "Amount is negative: " + message.getAmount();
        }
        if (message.getDescription() != null && message.getDescription().toUpperCase().contains("ERROR")) {
            return "Description contains ERROR keyword";
        }
        if (message.getClientId() != null && message.getClientId().toUpperCase().contains("ERROR")) {
            return "Client ID contains ERROR keyword";
        }
        return "Unknown validation error";
    }
}
