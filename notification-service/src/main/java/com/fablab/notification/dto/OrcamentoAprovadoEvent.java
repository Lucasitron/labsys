package com.fablab.notification.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Evento {@code orcamento.aprovado.event} consumido do Vendas Service. */
public record OrcamentoAprovadoEvent(
        Long idOrcamento,
        Long idCliente,
        BigDecimal valorTotal,
        LocalDate dataAprovacao) {
}
