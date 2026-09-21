package com.fablab.estoque.dto;

/**
 * Resposta de fornecedor.
 */
public record FornecedorResponse(
        Long id,
        String nome,
        String contato,
        String cnpj) {
}