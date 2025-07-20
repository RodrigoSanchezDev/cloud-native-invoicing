package com.sanchezdev.rabbitmqservice.service;

import java.util.concurrent.atomic.AtomicBoolean;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

import com.sanchezdev.rabbitmqservice.dto.InvoiceMessageDTO;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class AutoConsumerService {

    private final InvoiceConsumerService consumerService;
    private final Environment environment;
    
    // Control dinámico del auto-consumer
    private final AtomicBoolean isAutoConsumerEnabled = new AtomicBoolean(false);
    
    @PostConstruct
    public void initializeAutoConsumer() {
        // Verificar si el auto-consumer debe estar activo al inicio
        boolean enabledAtStartup = environment.getProperty("rabbitmq.auto-consumer.enabled", Boolean.class, false);
        isAutoConsumerEnabled.set(enabledAtStartup);
        log.info("AutoConsumerService initialized. Auto-consumer enabled at startup: {}", enabledAtStartup);
    }
    
    /**
     * Consumidor automático con control dinámico
     * Este listener siempre está activo, pero solo procesa mensajes si isAutoConsumerEnabled es true
     */
    @RabbitListener(queues = "invoice.queue")
    public void autoConsumeInvoiceMessage(InvoiceMessageDTO message) {
        // Verificar si el auto-consumer está habilitado dinámicamente
        if (!isAutoConsumerEnabled.get()) {
            log.debug("Auto-consumer is disabled. Message rejected and returned to queue.");
            // Rechazar el mensaje para que vuelva a la cola
            throw new RuntimeException("Auto-consumer is disabled");
        }
        
        log.info("Auto-consumer received and processing message: {}", message);
        consumerService.processInvoiceMessage(message);
    }
    
    /**
     * Activa el auto-consumer dinámicamente
     */
    public boolean enableAutoConsumer() {
        try {
            boolean wasEnabled = isAutoConsumerEnabled.getAndSet(true);
            if (wasEnabled) {
                log.info("Auto-consumer was already enabled");
            } else {
                log.info("Auto-consumer enabled successfully");
            }
            return true;
        } catch (Exception e) {
            log.error("Failed to enable auto-consumer: {}", e.getMessage(), e);
            return false;
        }
    }
    
    /**
     * Desactiva el auto-consumer dinámicamente
     */
    public boolean disableAutoConsumer() {
        try {
            boolean wasEnabled = isAutoConsumerEnabled.getAndSet(false);
            if (!wasEnabled) {
                log.info("Auto-consumer was already disabled");
            } else {
                log.info("Auto-consumer disabled successfully");
            }
            return true;
        } catch (Exception e) {
            log.error("Failed to disable auto-consumer: {}", e.getMessage(), e);
            return false;
        }
    }
    
    /**
     * Verifica si el auto-consumer está activo
     */
    public boolean isAutoConsumerActive() {
        return isAutoConsumerEnabled.get();
    }
    
    /**
     * Obtiene el estado detallado del auto-consumer
     */
    public String getAutoConsumerStatus() {
        if (isAutoConsumerActive()) {
            return "Auto-consumer is ACTIVE - messages are processed automatically";
        } else {
            return "Auto-consumer is DISABLED - messages will be rejected and returned to queue";
        }
    }
}
