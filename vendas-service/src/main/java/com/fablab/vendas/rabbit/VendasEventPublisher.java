package com.fablab.vendas.rabbit;

import com.fablab.vendas.config.RabbitConfig;
import com.fablab.vendas.rabbit.VendasEventos.EncomendaCriadaEvent;
import com.fablab.vendas.rabbit.VendasEventos.EncomendaStatusAlteradoEvent;
import com.fablab.vendas.rabbit.VendasEventos.OrcamentoAprovadoEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

/**
 * Publicadores de eventos do domínio de vendas (exchange {@code fablab.vendas}).
 */
@Component
public class VendasEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(VendasEventPublisher.class);

    private final RabbitTemplate rabbitTemplate;

    public VendasEventPublisher(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    /** Publica {@code encomenda.criada.event} (Produção). */
    public void encomendaCriada(EncomendaCriadaEvent event) {
        publicar(RabbitConfig.ENCOMENDA_CRIADA_ROUTING_KEY, event);
    }

    /** Publica {@code encomenda.status.alterado.event} (Produção + Notification). */
    public void encomendaStatusAlterado(EncomendaStatusAlteradoEvent event) {
        publicar(RabbitConfig.ENCOMENDA_STATUS_ROUTING_KEY, event);
    }

    /** Publica {@code orcamento.aprovado.event} (Financeiro). */
    public void orcamentoAprovado(OrcamentoAprovadoEvent event) {
        publicar(RabbitConfig.ORCAMENTO_APROVADO_ROUTING_KEY, event);
    }

    private void publicar(String routingKey, Object payload) {
        try {
            rabbitTemplate.convertAndSend(RabbitConfig.VENDAS_EXCHANGE, routingKey, payload);
        } catch (Exception ex) {
            log.warn("Falha ao publicar {}: {}", routingKey, ex.getMessage());
        }
    }
}
