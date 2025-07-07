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
            log.info("Sending invoice message to queue: {}", message);
            rabbitTemplate.convertAndSend(
                RabbitMQConfig.INVOICE_EXCHANGE,
                RabbitMQConfig.INVOICE_ROUTING_KEY,
                message
            );
            log.info("Invoice message sent successfully: Invoice ID {}", message.getInvoiceId());
        } catch (Exception e) {
            log.error("Error sending invoice message: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to send invoice message", e);
        }
    }
}
