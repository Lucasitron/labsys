package com.fablab.rh.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuração do RabbitMQ: consumo dos eventos de acesso do Auth Service e
 * publicação dos eventos de mudança de nível e validação de horas.
 */
@Configuration
public class RabbitMqConfig {

    /** Exchange tópica onde o Auth Service publica eventos de acesso. */
    public static final String ACCESS_EXCHANGE = "fablab.access";

    /** Routing key do evento de leitura RFID. */
    public static final String RFID_ROUTING_KEY = "access.rfid.event";

    /** Fila de registros de ponto consumida pelo Pessoas &amp; RH Service. */
    public static final String ACCESS_QUEUE = "rh.ponto.diario";

    /** Exchange tópica de domínio do Pessoas &amp; RH Service. */
    public static final String RH_EXCHANGE = "fablab.rh";

    /** Routing key do evento de alteração de nível de acesso. */
    public static final String NIVEL_ALTERADO_ROUTING_KEY = "nivel.alterado.event";

    /** Routing key do evento de validação de horas apontadas. */
    public static final String HORAS_VALIDADAS_ROUTING_KEY = "horas.validadas.event";

    /** Routing key do evento de solicitação de certificado de horas. */
    public static final String CERTIFICADO_SOLICITADO_ROUTING_KEY = "certificado.solicitado.event";

    /** Routing key do evento de aprovação de certificado de horas. */
    public static final String CERTIFICADO_APROVADO_ROUTING_KEY = "certificado.aprovado.event";

    /** Routing key do evento de rejeição de certificado de horas. */
    public static final String CERTIFICADO_REJEITADO_ROUTING_KEY = "certificado.rejeitado.event";

    /** Routing key do evento de extrato mensal de horas. */
    public static final String EXTRATO_MENSAL_HORAS_ROUTING_KEY = "extrato.mensal.horas.event";

    @Bean
    public TopicExchange accessExchange() {
        return new TopicExchange(ACCESS_EXCHANGE, true, false);
    }

    @Bean
    public Queue pontoQueue() {
        return new Queue(ACCESS_QUEUE, true);
    }

    @Bean
    public Binding pontoBinding(Queue pontoQueue, TopicExchange accessExchange) {
        return BindingBuilder.bind(pontoQueue).to(accessExchange).with(RFID_ROUTING_KEY);
    }

    @Bean
    public TopicExchange rhExchange() {
        return new TopicExchange(RH_EXCHANGE, true, false);
    }
}