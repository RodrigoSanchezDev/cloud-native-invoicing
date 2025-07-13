package com.sanchezdev.rabbitmqservice.service;

import com.sanchezdev.rabbitmqservice.config.RabbitMQConfig;
import com.sanchezdev.rabbitmqservice.dto.InvoiceMessageDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class RabbitMQProducerService {

    private final RabbitTemplate rabbitTemplate;

    public void sendInvoiceMessage(InvoiceMessageDTO message) {
        try {
            // Validar si el mensaje debe ir a DLQ por errores
            if (isErrorMessage(message)) {
                log.warn("Message contains errors, sending to DLQ: {}", message);
                sendErrorMessageToDLQ(message);
            } else {
                log.info("Sending valid invoice message to normal queue: {}", message);
                rabbitTemplate.convertAndSend(
                    RabbitMQConfig.INVOICE_EXCHANGE,
                    RabbitMQConfig.INVOICE_ROUTING_KEY,
                    message
                );
                log.info("Invoice message sent successfully to normal queue: Invoice ID {}", message.getInvoiceId());
            }
        } catch (Exception e) {
            log.error("Error sending invoice message: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to send invoice message", e);
        }
    }

    /**
     * Envía un mensaje directamente a la DLQ
     */
    public void sendErrorMessageToDLQ(InvoiceMessageDTO message) {
        try {
            log.info("Sending error message to DLQ: {}", message);
            rabbitTemplate.convertAndSend(
                RabbitMQConfig.INVOICE_DLQ_EXCHANGE,
                RabbitMQConfig.INVOICE_DLQ_ROUTING_KEY,
                message
            );
            log.info("Error message sent successfully to DLQ: Invoice ID {}", message.getInvoiceId());
        } catch (Exception e) {
            log.error("Error sending message to DLQ: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to send message to DLQ", e);
        }
    }

    /**
     * Valida si un mensaje contiene errores y debe ir a DLQ
     */
    private boolean isErrorMessage(InvoiceMessageDTO message) {
        // Validaciones para determinar si va a DLQ
        if (message.getClientId() == null || message.getClientId().trim().isEmpty()) {
            log.warn("Message missing clientId, will go to DLQ");
            return true;
        }
        
        if (message.getDescription() != null && message.getDescription().contains("ERROR")) {
            log.warn("Message contains 'ERROR' in description, will go to DLQ");
            return true;
        }
        
        if (message.getClientId() != null && message.getClientId().contains("ERROR")) {
            log.warn("Message contains 'ERROR' in clientId, will go to DLQ");
            return true;
        }
        
        // Validar monto negativo (si existe)
        if (message.getAmount() != null && message.getAmount() < 0) {
            log.warn("Message has negative amount, will go to DLQ");
            return true;
        }
        
        // Validar status de error
        if (message.getStatus() != null && message.getStatus().contains("ERROR")) {
            log.warn("Message has ERROR status, will go to DLQ");
            return true;
        }
        
        return false;
    }
}
