package com.fablab.financeiro.consumer;

import com.fablab.financeiro.config.RabbitMqConfig;
import com.fablab.financeiro.dto.EncomendaCriadaEvent;
import com.fablab.financeiro.service.FechamentoEncomendaService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * Consome {@code encomenda.criada.event} do Vendas &amp; CRM e abre o
 * fechamento da encomenda em {@code ABERTA}.
 */
@Component
public class EncomendaCriadaListener {

    private static final Logger log = LoggerFactory.getLogger(EncomendaCriadaListener.class);

    private final FechamentoEncomendaService fechamentoEncomendaService;

    public EncomendaCriadaListener(FechamentoEncomendaService fechamentoEncomendaService) {
        this.fechamentoEncomendaService = fechamentoEncomendaService;
    }

    @RabbitListener(queues = RabbitMqConfig.ENCOMENDA_CRIADA_QUEUE)
    public void aoCriarEncomenda(EncomendaCriadaEvent event) {
        try {
            fechamentoEncomendaService.criarDoEvento(event);
            log.info("Fechamento aberto para a encomenda {}", event.idEncomenda());
        } catch (RuntimeException ex) {
            log.warn("Falha ao abrir fechamento da encomenda {}: {}",
                    event.idEncomenda(), ex.getMessage());
        }
    }
}