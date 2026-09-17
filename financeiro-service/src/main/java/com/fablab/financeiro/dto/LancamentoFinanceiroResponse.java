package com.fablab.financeiro.dto;

import com.fablab.financeiro.entity.LancamentoFinanceiro;
import com.fablab.financeiro.entity.StatusLancamento;
import com.fablab.financeiro.entity.TipoLancamento;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Visão de um lançamento financeiro.
 */
public record LancamentoFinanceiroResponse(
        Long idLancamento,
        Long idCategoria,
        String categoriaNome,
        TipoLancamento tipo,
        BigDecimal valor,
        LocalDate dataVencimento,
        LocalDate dataPagamento,
        StatusLancamento status,
        String idReferenciaExterna,
        String observacao) {

    public static LancamentoFinanceiroResponse of(LancamentoFinanceiro lancamento) {
        return new LancamentoFinanceiroResponse(
                lancamento.getIdLancamento(),
                lancamento.getCategoria().getIdCategoria(),
                lancamento.getCategoria().getNome(),
                lancamento.getTipo(),
                lancamento.getValor(),
                lancamento.getDataVencimento(),
                lancamento.getDataPagamento(),
                lancamento.getStatus(),
                lancamento.getIdReferenciaExterna(),
                lancamento.getObservacao());
    }
}