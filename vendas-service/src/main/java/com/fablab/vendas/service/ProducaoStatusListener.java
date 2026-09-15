package com.fablab.vendas.service;

import com.fablab.vendas.config.RabbitMqConfig;
import com.fablab.vendas.dto.ProducaoStatusEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * Consome {@code producao.status.alterado.event} do Produção &amp; Projetos Service para
 * atualizar automaticamente o Kanban da encomenda.
 */
@Component
public class ProducaoStatusListener {

    private static final Logger log = LoggerFactory.getLogger(ProducaoStatusListener.class);

    private final EncomendaService encomendaService;

    public ProducaoStatusListener(EncomendaService encomendaService) {
        this.encomendaService = encomendaService;
    }

    @RabbitListener(queues = RabbitMqConfig.KANBAN_PRODUCAO_QUEUE)
    public void onProducaoStatus(ProducaoStatusEvent event) {
        log.info("Evento producao.status.alterado.event recebido para encomenda {}: {}",
                event.idEncomenda(), event.statusNovo());
        try {
            encomendaService.receberAtualizacaoProducao(event);
        } catch (RuntimeException ex) {
            log.error("Erro ao processar atualização de produção para encomenda {}: {}",
                    event.idEncomenda(), ex.getMessage(), ex);
        }
    }
}