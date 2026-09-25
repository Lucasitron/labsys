package com.fablab.financeiro.service;

import com.fablab.financeiro.config.RabbitMqConfig;
import com.fablab.financeiro.dto.FinanceiroEventos.CompraSolicitadaEvent;
import com.fablab.financeiro.dto.FinanceiroEventos.CustoCalculadoEvent;
import com.fablab.financeiro.dto.FinanceiroEventos.LancamentoVencidoEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

/** Publicadores de eventos do domínio financeiro (exchange {@code fablab.financeiro}). */
@Component
public class FinanceiroEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(FinanceiroEventPublisher.class);

    private final RabbitTemplate rabbitTemplate;

    public FinanceiroEventPublisher(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    /** Publica {@code lancamento.vencido.event} (→ Notification). */
    public void publishLancamentoVencido(LancamentoVencidoEvent event) {
        publicar(RabbitMqConfig.LANCAMENTO_VENCIDO_ROUTING_KEY, event);
    }

    /** Publica {@code custo.calculado.event} (→ Vendas &amp; CRM). */
    public void publishCustoCalculado(CustoCalculadoEvent event) {
        publicar(RabbitMqConfig.CUSTO_CALCULADO_ROUTING_KEY, event);
    }

    /** Publica {@code compra.solicitada.event} (→ Estoque &amp; Suprimentos). */
    public void publishCompraSolicitada(CompraSolicitadaEvent event) {
        publicar(RabbitMqConfig.COMPRA_SOLICITADA_ROUTING_KEY, event);
    }

    private void publicar(String routingKey, Object payload) {
        try {
            rabbitTemplate.convertAndSend(RabbitMqConfig.FINANCEIRO_EXCHANGE, routingKey, payload);
        } catch (Exception ex) {
            log.warn("Falha ao publicar {}: {}", routingKey, ex.getMessage());
        }
    }
}
