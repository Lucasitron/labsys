package com.fablab.estoque.dto;

import com.fablab.estoque.entity.Categoria;
import java.math.BigDecimal;

/**
 * Resposta de item de inventário.
 */
public record ItemResponse(
        Long id,
        String nome,
        String descricao,
        Categoria categoria,
        String unidadeMedida,
        BigDecimal quantidadeAtual,
        BigDecimal estoqueMinimo,
        LocalizacaoResponse localizacao) {
}