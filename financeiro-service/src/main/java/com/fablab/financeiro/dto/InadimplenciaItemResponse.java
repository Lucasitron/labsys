package com.fablab.financeiro.dto;

import com.fablab.financeiro.entity.LancamentoFinanceiro;
import com.fablab.financeiro.entity.StatusLancamento;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Item do relatório de inadimplência (contas a receber vencidas e pendentes).
 */
public record InadimplenciaItemResponse(
        Long idLancamento,
        String categoriaNome,
        BigDecimal valor,
        LocalDate dataVencimento,
        StatusLancamento status) {

    public static InadimplenciaItemResponse of(LancamentoFinanceiro lancamento) {
        return new InadimplenciaItemResponse(
                lancamento.getIdLancamento(),
                lancamento.getCategoria().getNome(),
                lancamento.getValor(),
                lancamento.getDataVencimento(),
                lancamento.getStatus());
    }
}