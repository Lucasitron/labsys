package com.fablab.vendas.dto;

import com.fablab.vendas.entity.ItemOrcamento;
import java.math.BigDecimal;

/**
 * Item de um orçamento na resposta.
 */
public record ItemOrcamentoResponse(
        Long id,
        String descricao,
        BigDecimal quantidade,
        BigDecimal valorUnitario,
        BigDecimal subtotal) {

    public static ItemOrcamentoResponse of(ItemOrcamento item) {
        return new ItemOrcamentoResponse(
                item.getId(),
                item.getDescricao(),
                item.getQuantidade(),
                item.getValorUnitario(),
                item.getSubtotal());
    }
}