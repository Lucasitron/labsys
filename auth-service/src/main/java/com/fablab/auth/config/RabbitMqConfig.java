package com.fablab.auth.config;

import com.fablab.auth.dto.RfidAccessEvent;

import org.springframework.amqp.core.TopicExchange;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuração do RabbitMQ para publicação de eventos de acesso físico.
 */
@Configuration
public class RabbitMqConfig {

    /** Exchange tópica onde os eventos de acesso são publicados. */
    public static final String ACCESS_EXCHANGE = "fablab.access";

    /** Routing key do evento de leitura RFID. */
    public static final String RFID_ROUTING_KEY = "access.rfid.event";

    /**
     * Exchange de eventos de acesso.
     *
     * <p>{@link RfidAccessEvent} é publicado aqui e consumido pelo Pessoas &amp;
     * RH Service para registro de ponto.</p>
     */
    @Bean
    public TopicExchange accessExchange() {
        return new TopicExchange(ACCESS_EXCHANGE, true, false);
    }
}