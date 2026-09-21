package com.fablab.estoque.dto;

import java.util.List;

/**
 * Resposta de Lista de Materiais (BOM).
 */
public record BomResponse(
        Long id,
        Long idProdutoServico,
        String nome,
        Integer versao,
        Boolean editavel,
        List<BomItemResponse> itens) {
}