package com.fablab.rh.service;

import com.fablab.rh.config.RabbitMqConfig;
import com.fablab.rh.dto.HorasValidadasEvent;
import com.fablab.rh.dto.NivelAlteradoEvent;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

/**
 * Publica eventos assíncronos do domínio no RabbitMQ.
 */
@Service
public class RhEventPublisher {

    private final RabbitTemplate rabbitTemplate;

    public RhEventPublisher(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    /** Publica a alteração de nível de acesso para o Auth &amp; Identity Service. */
    public void publishNivelAlterado(NivelAlteradoEvent event) {
        rabbitTemplate.convertAndSend(
                RabbitMqConfig.RH_EXCHANGE,
                RabbitMqConfig.NIVEL_ALTERADO_ROUTING_KEY,
                event);
    }

    /** Publica as horas validadas para o Financeiro Service. */
    public void publishHorasValidadas(HorasValidadasEvent event) {
        rabbitTemplate.convertAndSend(
                RabbitMqConfig.RH_EXCHANGE,
                RabbitMqConfig.HORAS_VALIDADAS_ROUTING_KEY,
                event);
    }
}