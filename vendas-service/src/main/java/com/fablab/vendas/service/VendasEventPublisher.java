package com.fablab.vendas.service;

import com.fablab.vendas.config.RabbitMqConfig;
import com.fablab.vendas.dto.EncomendaCriadaEvent;
import com.fablab.vendas.dto.EncomendaEntregueEvent;
import com.fablab.vendas.dto.KanbanStatusEvent;
import com.fablab.vendas.dto.MarketplaceVendaEvent;
import com.fablab.vendas.dto.OrcamentoAprovadoEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

/**
 * Publica eventos de domínio do Vendas &amp; CRM Service no RabbitMQ.
 */
@Service
public class VendasEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(VendasEventPublisher.class);

    private final RabbitTemplate rabbitTemplate;

    public VendasEventPublisher(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    public void publishOrcamentoAprovado(OrcamentoAprovadoEvent event) {
        send(RabbitMqConfig.VENDAS_EXCHANGE, RabbitMqConfig.ORCAMENTO_APROVADO_ROUTING_KEY,
                "orcamento.aprovado", event.idOrcamento(), event);
    }

    public void publishEncomendaCriada(EncomendaCriadaEvent event) {
        send(RabbitMqConfig.VENDAS_EXCHANGE, RabbitMqConfig.ENCOMENDA_CRIADA_ROUTING_KEY,
                "encomenda.criada", event.idEncomenda(), event);
    }

    public void publishEncomendaEntregue(EncomendaEntregueEvent event) {
        send(RabbitMqConfig.VENDAS_EXCHANGE, RabbitMqConfig.ENCOMENDA_ENTREGUE_ROUTING_KEY,
                "encomenda.entregue", event.idEncomenda(), event);
    }

    public void publishMarketplaceVenda(MarketplaceVendaEvent event) {
        send(RabbitMqConfig.VENDAS_EXCHANGE, RabbitMqConfig.MARKETPLACE_VENDA_ROUTING_KEY,
                "marketplace.venda", event.idRegistro(), event);
    }

    public void publishKanbanStatus(KanbanStatusEvent event) {
        send(RabbitMqConfig.VENDAS_EXCHANGE, RabbitMqConfig.ENCOMENDA_STATUS_ALTERADO_ROUTING_KEY,
                "encomenda.status.alterado", event.idEncomenda(), event);
    }

    private void send(String exchange, String routingKey, String evento, Object id, Object event) {
        try {
            rabbitTemplate.convertAndSend(exchange, routingKey, event);
            log.info("Evento {} publicado (id={})", evento, id);
        } catch (RuntimeException ex) {
            log.warn("Falha ao publicar {} (id={}): {}", evento, id, ex.getMessage());
        }
    }
}