package com.fablab.financeiro.consumer;

import com.fablab.financeiro.config.RabbitMqConfig;
import com.fablab.financeiro.dto.FinanceiroEventos.ProducaoConcluidaEvent;
import com.fablab.financeiro.service.CusteioService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/** Consumidor de {@code producao.concluida.event}: dispara o cálculo de custo da encomenda. */
@Component
public class ProducaoConcluidaListener {

    private static final Logger log = LoggerFactory.getLogger(ProducaoConcluidaListener.class);

    private final CusteioService custeioService;

    public ProducaoConcluidaListener(CusteioService custeioService) {
        this.custeioService = custeioService;
    }

    @RabbitListener(queues = RabbitMqConfig.CUSTO_QUEUE)
    public void onProducaoConcluida(ProducaoConcluidaEvent event) {
        try {
            if (event == null || event.idEncomenda() == null) {
                log.warn("Evento de produção concluída malformado, ignorado");
                return;
            }
            custeioService.calcularDoEvento(event);
        } catch (Exception ex) {
            log.error("Falha ao processar producao.concluida.event: {}", ex.getMessage(), ex);
        }
    }
}
