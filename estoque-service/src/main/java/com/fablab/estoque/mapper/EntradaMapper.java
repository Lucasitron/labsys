package com.fablab.estoque.mapper;

import com.fablab.estoque.dto.EntradaResponse;
import com.fablab.estoque.entity.EntradaEstoque;

/**
 * Mapeia entidades {@code EntradaEstoque} para DTOs.
 */
public final class EntradaMapper {

    private EntradaMapper() {
    }

    public static EntradaResponse toResponse(EntradaEstoque entrada) {
        return new EntradaResponse(
                entrada.getId(),
                entrada.getItem().getId(),
                entrada.getItem().getNome(),
                entrada.getFornecedor().getId(),
                entrada.getFornecedor().getNome(),
                entrada.getQuantidade(),
                entrada.getValorUnitario(),
                entrada.getValorTotal(),
                entrada.getDataEntrada(),
                entrada.getNotaFiscal(),
                entrada.getObservacao());
    }
}