package com.sanchezdev.invoiceservice.service;

import com.sanchezdev.invoiceservice.config.RabbitMQConfig;
import com.sanchezdev.invoiceservice.dto.InvoiceMessageDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(name = "rabbitmq.enabled", havingValue = "true", matchIfMissing = false)
public class RabbitMQProducerService {

    private final RabbitTemplate rabbitTemplate;

    public void sendInvoiceCreatedMessage(InvoiceMessageDTO message) {
        try {
            log.info("Sending invoice created message to RabbitMQ: {}", message);
            rabbitTemplate.convertAndSend(
                RabbitMQConfig.INVOICE_EXCHANGE,
                RabbitMQConfig.INVOICE_ROUTING_KEY,
                message
            );
            log.info("Invoice message sent successfully to RabbitMQ: Invoice ID {}", message.getInvoiceId());
        } catch (Exception e) {
            log.error("Error sending invoice message to RabbitMQ: {}", e.getMessage(), e);
            // No lanzamos excepción para no afectar el flujo principal de creación de facturas
        }
    }
}
