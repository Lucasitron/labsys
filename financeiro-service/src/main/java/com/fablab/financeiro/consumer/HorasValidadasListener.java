package com.fablab.financeiro.consumer;

import com.fablab.financeiro.config.RabbitMqConfig;
import com.fablab.financeiro.dto.FinanceiroEventos.HorasValidadasEvent;
import com.fablab.financeiro.service.FechamentoEncomendaService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/** Consumidor de {@code horas.validadas.event}: acumula horas por (encomenda, funcionário, data). */
@Component
public class HorasValidadasListener {

    private static final Logger log = LoggerFactory.getLogger(HorasValidadasListener.class);

    private final FechamentoEncomendaService fechamentoService;

    public HorasValidadasListener(FechamentoEncomendaService fechamentoService) {
        this.fechamentoService = fechamentoService;
    }

    @RabbitListener(queues = RabbitMqConfig.HORAS_QUEUE)
    public void onHorasValidadas(HorasValidadasEvent event) {
        try {
            if (event == null || event.idEncomenda() == null || event.horas() == null) {
                log.warn("Evento de horas validadas malformado, ignorado");
                return;
            }
            fechamentoService.registrarHorasValidadas(event);
        } catch (Exception ex) {
            log.error("Falha ao processar horas.validadas.event: {}", ex.getMessage(), ex);
        }
    }
}
