package com.fablab.vendas.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Evento {@code encomenda.entregue.event} publicado para o Financeiro Service
 * registrar a entrada.
 */
public record EncomendaEntregueEvent(
        Long idEncomenda,
        Long idCliente,
        BigDecimal valorFinal,
        LocalDate dataEntrega) {
}