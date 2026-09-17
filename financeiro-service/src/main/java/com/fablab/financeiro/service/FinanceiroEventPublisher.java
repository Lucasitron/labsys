package com.fablab.financeiro.service;

import com.fablab.financeiro.config.RabbitMqConfig;
import com.fablab.financeiro.dto.CompraSolicitadaEvent;
import com.fablab.financeiro.dto.CustoCalculadoEvent;
import com.fablab.financeiro.dto.LancamentoVencidoEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

/**
 * Publica eventos de domínio do Financeiro Service no RabbitMQ.
 */
@Service
public class FinanceiroEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(FinanceiroEventPublisher.class);

    private final RabbitTemplate rabbitTemplate;

    public FinanceiroEventPublisher(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    /** Notifica o Notification Service de lançamentos vencidos. */
    public void publishLancamentoVencido(LancamentoVencidoEvent event) {
        send(RabbitMqConfig.LANCAMENTO_VENCIDO_ROUTING_KEY,
                "lancamento.vencido", event.idLancamento(), event);
    }

    /** Informa o Vendas &amp; CRM do custo calculado de uma encomenda. */
    public void publishCustoCalculado(CustoCalculadoEvent event) {
        send(RabbitMqConfig.CUSTO_CALCULADO_ROUTING_KEY,
                "custo.calculado", event.idEncomenda(), event);
    }

    /** Informa o Estoque &amp; Suprimentos de uma solicitação de compra. */
    public void publishCompraSolicitada(CompraSolicitadaEvent event) {
        send(RabbitMqConfig.COMPRA_SOLICITADA_ROUTING_KEY,
                "compra.solicitada", event.idCompra(), event);
    }

    private void send(String routingKey, String evento, Object id, Object event) {
        try {
            rabbitTemplate.convertAndSend(RabbitMqConfig.FINANCEIRO_EXCHANGE, routingKey, event);
            log.info("Evento {} publicado (id={})", evento, id);
        } catch (RuntimeException ex) {
            log.warn("Falha ao publicar {} (id={}): {}", evento, id, ex.getMessage());
        }
    }
}