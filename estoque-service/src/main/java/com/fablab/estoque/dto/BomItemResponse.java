package com.fablab.estoque.dto;

import java.math.BigDecimal;

/**
 * Item de uma Lista de Materiais (BOM) na resposta.
 */
public record BomItemResponse(
        Long idItem,
        String nomeItem,
        BigDecimal quantidadePrevista,
        BigDecimal quantidadeReal) {
}