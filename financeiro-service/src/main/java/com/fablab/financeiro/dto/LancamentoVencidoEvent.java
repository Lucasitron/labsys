package com.fablab.financeiro.dto;

import com.fablab.financeiro.entity.TipoLancamento;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Evento {@code lancamento.vencido.event} publicado para o Notification Service
 * quando um lançamento vence sem ser liquidado.
 *
 * @param idLancamento id do lançamento
 * @param tipo entrada (a receber) ou saída (a pagar)
 * @param valor valor do lançamento
 * @param dataVencimento data de vencimento
 */
public record LancamentoVencidoEvent(
        Long idLancamento,
        TipoLancamento tipo,
        BigDecimal valor,
        LocalDate dataVencimento) {
}