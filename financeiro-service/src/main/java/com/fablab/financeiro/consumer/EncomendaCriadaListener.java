package com.fablab.financeiro.consumer;

import com.fablab.financeiro.config.RabbitMqConfig;
import com.fablab.financeiro.dto.FinanceiroEventos.EncomendaCriadaEvent;
import com.fablab.financeiro.service.FechamentoEncomendaService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/** Consumidor idempotente de {@code encomenda.criada.event}: abre o fechamento em ABERTA. */
@Component
public class EncomendaCriadaListener {

    private static final Logger log = LoggerFactory.getLogger(EncomendaCriadaListener.class);

    private final FechamentoEncomendaService fechamentoService;

    public EncomendaCriadaListener(FechamentoEncomendaService fechamentoService) {
        this.fechamentoService = fechamentoService;
    }

    @RabbitListener(queues = RabbitMqConfig.FECHAMENTO_QUEUE)
    public void onEncomendaCriada(EncomendaCriadaEvent event) {
        try {
            if (event == null || event.idEncomenda() == null) {
                log.warn("Evento de encomenda criada malformado, ignorado");
                return;
            }
            fechamentoService.criarDoEvento(event);
        } catch (Exception ex) {
            log.error("Falha ao processar encomenda.criada.event: {}", ex.getMessage(), ex);
        }
    }
}
