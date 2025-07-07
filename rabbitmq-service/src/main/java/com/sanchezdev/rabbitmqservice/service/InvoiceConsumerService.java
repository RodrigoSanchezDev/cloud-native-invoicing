package com.sanchezdev.rabbitmqservice.service;

import com.sanchezdev.rabbitmqservice.dto.InvoiceMessageDTO;
import com.sanchezdev.rabbitmqservice.model.BoletaOracle;
import com.sanchezdev.rabbitmqservice.repository.BoletaOracleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class InvoiceConsumerService {

    private final BoletaOracleRepository boletaOracleRepository;

    @RabbitListener(queues = "invoice.queue")
    public void processInvoiceMessage(InvoiceMessageDTO message) {
        try {
            log.info("Processing invoice message: {}", message);
            
            // Crear nueva boleta para Oracle Cloud
            BoletaOracle boleta = new BoletaOracle();
            boleta.setClientId(message.getClientId());
            boleta.setInvoiceDate(message.getInvoiceDate());
            boleta.setFileName(message.getFileName());
            boleta.setS3Key(message.getS3Key());
            boleta.setOriginalInvoiceId(message.getInvoiceId());
            boleta.setDescription("Boleta procesada desde cola MQ - " + message.getDescription());
            boleta.setStatus("PROCESSED");
            
            // Guardar en Oracle Cloud
            BoletaOracle savedBoleta = boletaOracleRepository.save(boleta);
            
            log.info("Invoice message processed successfully: Oracle Boleta ID {}, Original Invoice ID {}", 
                     savedBoleta.getId(), message.getInvoiceId());
            
        } catch (Exception e) {
            log.error("Error processing invoice message: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to process invoice message", e);
        }
    }

    public List<BoletaOracle> getAllBoletas() {
        return boletaOracleRepository.findAllOrderByProcessedDateDesc();
    }

    public List<BoletaOracle> getBoletasByClient(String clientId) {
        return boletaOracleRepository.findByClientId(clientId);
    }

    public List<BoletaOracle> getBoletasByOriginalInvoiceId(Long invoiceId) {
        return boletaOracleRepository.findByOriginalInvoiceId(invoiceId);
    }
}
