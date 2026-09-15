package com.fablab.vendas.dto;

import com.fablab.vendas.entity.Orcamento;
import com.fablab.vendas.entity.StatusOrcamento;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Resposta de orçamento, incluindo os itens e o valor total.
 */
public record OrcamentoResponse(
        Long id,
        Long idCliente,
        String clienteNome,
        LocalDate dataCriacao,
        LocalDate validade,
        BigDecimal valorTotal,
        StatusOrcamento status,
        String observacoes,
        List<ItemOrcamentoResponse> itens) {

    public static OrcamentoResponse of(Orcamento orcamento) {
        List<ItemOrcamentoResponse> itens = orcamento.getItens() == null ? List.of()
                : orcamento.getItens().stream().map(ItemOrcamentoResponse::of).toList();
        return new OrcamentoResponse(
                orcamento.getId(),
                orcamento.getCliente().getId(),
                orcamento.getCliente().getNomeRazaoSocial(),
                orcamento.getDataCriacao(),
                orcamento.getValidade(),
                orcamento.getValorTotal(),
                orcamento.getStatus(),
                orcamento.getObservacoes(),
                itens);
    }
}