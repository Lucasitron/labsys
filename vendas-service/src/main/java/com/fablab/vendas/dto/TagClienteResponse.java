package com.fablab.vendas.dto;

import com.fablab.vendas.entity.TagCliente;

/**
 * Resposta de tag de cliente.
 */
public record TagClienteResponse(Long id, String nome, String cor) {

    public static TagClienteResponse of(TagCliente tag) {
        return new TagClienteResponse(tag.getId(), tag.getNome(), tag.getCor());
    }
}