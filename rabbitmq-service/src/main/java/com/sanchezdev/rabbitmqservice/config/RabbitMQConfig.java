package com.sanchezdev.rabbitmqservice.config;

import jakarta.annotation.PostConstruct;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.annotation.EnableRabbit;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.DefaultClassMapper;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

@Configuration
@EnableRabbit
public class RabbitMQConfig {

    public static final String INVOICE_QUEUE = "invoice.queue";
    public static final String INVOICE_EXCHANGE = "invoice.exchange";
    public static final String INVOICE_ROUTING_KEY = "invoice.created";
    
    // 🆕 DLQ Configuration - Para manejo de errores
    public static final String INVOICE_DLQ = "invoice.dlq";
    public static final String INVOICE_DLQ_EXCHANGE = "invoice.dlq.exchange";
    public static final String INVOICE_DLQ_ROUTING_KEY = "invoice.error";

    @Bean
    public Queue invoiceQueue() {
        System.out.println("🔧 Creating RabbitMQ Queue: " + INVOICE_QUEUE);
        Queue queue = QueueBuilder
                .durable(INVOICE_QUEUE)
                .build();
        System.out.println("✅ Queue created successfully: " + queue.getName());
        return queue;
    }
    
    // 🆕 DLQ Queue - Para mensajes con errores
    @Bean
    public Queue invoiceDLQ() {
        System.out.println("🔧 Creating RabbitMQ DLQ: " + INVOICE_DLQ);
        Queue dlq = QueueBuilder
                .durable(INVOICE_DLQ)
                .build();
        System.out.println("✅ DLQ created successfully: " + dlq.getName());
        return dlq;
    }

    @Bean
    public DirectExchange invoiceExchange() {
        System.out.println("🔧 Creating RabbitMQ Exchange: " + INVOICE_EXCHANGE);
        DirectExchange exchange = new DirectExchange(INVOICE_EXCHANGE);
        System.out.println("✅ Exchange created successfully: " + exchange.getName());
        return exchange;
    }
    
    // 🆕 DLQ Exchange - Para enrutamiento de errores
    @Bean
    public DirectExchange invoiceDLQExchange() {
        System.out.println("🔧 Creating RabbitMQ DLQ Exchange: " + INVOICE_DLQ_EXCHANGE);
        DirectExchange dlqExchange = new DirectExchange(INVOICE_DLQ_EXCHANGE);
        System.out.println("✅ DLQ Exchange created successfully: " + dlqExchange.getName());
        return dlqExchange;
    }

    @Bean
    public Binding invoiceBinding(Queue invoiceQueue, DirectExchange invoiceExchange) {
        System.out.println("🔧 Creating RabbitMQ Binding: " + INVOICE_QUEUE + " -> " + INVOICE_EXCHANGE + " [" + INVOICE_ROUTING_KEY + "]");
        Binding binding = BindingBuilder
                .bind(invoiceQueue)
                .to(invoiceExchange)
                .with(INVOICE_ROUTING_KEY);
        System.out.println("✅ Binding created successfully");
        return binding;
    }
    
    // 🆕 DLQ Binding - Para mensajes de error
    @Bean
    public Binding invoiceDLQBinding() {
        System.out.println("🔧 Creating RabbitMQ DLQ Binding: " + INVOICE_DLQ + " -> " + INVOICE_DLQ_EXCHANGE + " [" + INVOICE_DLQ_ROUTING_KEY + "]");
        Binding dlqBinding = BindingBuilder
                .bind(invoiceDLQ())
                .to(invoiceDLQExchange())
                .with(INVOICE_DLQ_ROUTING_KEY);
        System.out.println("✅ DLQ Binding created successfully");
        return dlqBinding;
    }

    @Bean
    public Jackson2JsonMessageConverter messageConverter() {
        Jackson2JsonMessageConverter converter = new Jackson2JsonMessageConverter();
        
        // Configuración más simple para evitar problemas de deserialización
        DefaultClassMapper classMapper = new DefaultClassMapper();
        classMapper.setTrustedPackages("*"); // Permite todos los paquetes para deserialización
        
        // Mapeo simple para manejar el tipo sin problemas de rutas de clases
        Map<String, Class<?>> typeIdMappings = new HashMap<>();
        typeIdMappings.put("com.sanchezdev.invoiceservice.dto.InvoiceMessageDTO", 
                          com.sanchezdev.rabbitmqservice.dto.InvoiceMessageDTO.class);
        classMapper.setIdClassMapping(typeIdMappings);
        
        converter.setClassMapper(classMapper);
        
        return converter;
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(messageConverter());
        return template;
    }

    @Bean
    public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(ConnectionFactory connectionFactory) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setMessageConverter(messageConverter());
        return factory;
    }

    @Bean
    public RabbitAdmin rabbitAdmin(ConnectionFactory connectionFactory) {
        System.out.println("🔧 Creating RabbitAdmin for automatic declaration");
        RabbitAdmin admin = new RabbitAdmin(connectionFactory);
        admin.setAutoStartup(true);
        System.out.println("✅ RabbitAdmin created successfully");
        return admin;
    }

    @PostConstruct
    public void verifyRabbitConnection() {
        System.out.println("🚀 RabbitMQConfig initialized - Exchange: " + INVOICE_EXCHANGE + ", Queue: " + INVOICE_QUEUE);
    }
}
