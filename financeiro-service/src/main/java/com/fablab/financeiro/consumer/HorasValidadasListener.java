package com.fablab.financeiro.consumer;

import com.fablab.financeiro.config.RabbitMqConfig;
import com.fablab.financeiro.dto.HorasValidadasEvent;
import com.fablab.financeiro.service.FechamentoEncomendaService;
import java.math.BigDecimal;
import java.util.Locale;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * Consome {@code horas.validadas.event} do Pessoas &amp; RH e acumula as horas
 * da encomenda para alimentar o custeio por ordem.
 */
@Component
public class HorasValidadasListener {

    private static final Logger log = LoggerFactory.getLogger(HorasValidadasListener.class);

    private final FechamentoEncomendaService fechamentoEncomendaService;

    public HorasValidadasListener(FechamentoEncomendaService fechamentoEncomendaService) {
        this.fechamentoEncomendaService = fechamentoEncomendaService;
    }

    @RabbitListener(queues = RabbitMqConfig.HORAS_VALIDADAS_QUEUE)
    public void aoValidarHoras(HorasValidadasEvent event) {
        try {
            if (!"ENCOMENDA".equalsIgnoreCase(event.tipo() == null ? "" : event.tipo().toUpperCase(Locale.ROOT))) {
                return;
            }
            BigDecimal horas = event.horas() != null ? event.horas() : BigDecimal.ZERO;
            fechamentoEncomendaService.registrarHorasValidadas(
                    event.idReferencia(), event.idFuncionario(), null, horas, event.data());
            log.info("Horas validadas acumuladas para a encomenda {}", event.idReferencia());
        } catch (RuntimeException ex) {
            log.warn("Falha ao acumular horas da encomenda {}: {}",
                    event.idReferencia(), ex.getMessage());
        }
    }
}