package com.fablab.auth.service;

import com.fablab.auth.config.RabbitMqConfig;
import com.fablab.auth.dto.RfidAccessEvent;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

/**
 * Publica eventos assíncronos no RabbitMQ.
 */
@Service
public class RfidEventPublisher {

    private final RabbitTemplate rabbitTemplate;

    public RfidEventPublisher(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    /**
     * Publica o evento de leitura RFID válida para o Pessoas &amp; RH Service.
     */
    public void publishAccess(RfidAccessEvent event) {
        rabbitTemplate.convertAndSend(
                RabbitMqConfig.ACCESS_EXCHANGE,
                RabbitMqConfig.RFID_ROUTING_KEY,
                event);
    }
}