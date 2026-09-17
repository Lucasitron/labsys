package com.fablab.financeiro.dto;

import com.fablab.financeiro.entity.SolicitacaoCompra;
import com.fablab.financeiro.entity.StatusSolicitacaoCompra;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Visão de uma solicitação de compra.
 */
public record SolicitacaoCompraResponse(
        Long idSolicitacao,
        Long idItemEstoque,
        BigDecimal quantidade,
        BigDecimal valorEstimado,
        StatusSolicitacaoCompra status,
        LocalDate dataSolicitacao) {

    public static SolicitacaoCompraResponse of(SolicitacaoCompra solicitacao) {
        return new SolicitacaoCompraResponse(
                solicitacao.getIdSolicitacao(),
                solicitacao.getIdItemEstoque(),
                solicitacao.getQuantidade(),
                solicitacao.getValorEstimado(),
                solicitacao.getStatus(),
                solicitacao.getDataSolicitacao());
    }
}