package com.fablab.financeiro.dto;

import com.fablab.financeiro.entity.FechamentoEncomenda;
import com.fablab.financeiro.entity.StatusFechamentoEncomenda;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Visão de um fechamento de encomenda.
 */
public record FechamentoEncomendaResponse(
        Long idFechamento,
        Long idEncomenda,
        BigDecimal horasEstimadas,
        BigDecimal horasValidadas,
        BigDecimal valorFechado,
        LocalDate dataFechamento,
        StatusFechamentoEncomenda status) {

    public static FechamentoEncomendaResponse of(FechamentoEncomenda fechamento) {
        return new FechamentoEncomendaResponse(
                fechamento.getIdFechamento(),
                fechamento.getIdEncomenda(),
                fechamento.getHorasEstimadas(),
                fechamento.getHorasValidadas(),
                fechamento.getValorFechado(),
                fechamento.getDataFechamento(),
                fechamento.getStatus());
    }
}