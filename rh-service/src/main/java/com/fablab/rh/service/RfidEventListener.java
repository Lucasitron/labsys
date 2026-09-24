package com.fablab.rh.service;

import com.fablab.rh.config.RabbitMqConfig;
import com.fablab.rh.dto.RfidAccessEvent;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * Escuta a fila de eventos de acesso físico publicados pelo Auth &amp; Identity
 * Service.
 */
@Component
public class RfidEventListener {

    private final PontoService pontoService;

    public RfidEventListener(PontoService pontoService) {
        this.pontoService = pontoService;
    }

    @RabbitListener(queues = RabbitMqConfig.ACCESS_QUEUE)
    public void onRfidEvent(RfidAccessEvent event) {
        pontoService.processarEventoRfid(event);
    }
}