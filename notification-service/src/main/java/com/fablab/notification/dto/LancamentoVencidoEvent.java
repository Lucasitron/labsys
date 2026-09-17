package com.fablab.notification.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Evento {@code lancamento.vencido.event} consumido do Financeiro Service. */
public record LancamentoVencidoEvent(
        Long idLancamento,
        String tipo,
        BigDecimal valor,
        LocalDate dataVencimento) {
}
