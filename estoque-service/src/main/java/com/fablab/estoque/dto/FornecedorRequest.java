package com.fablab.estoque.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Requisição de cadastro de fornecedor.
 */
public record FornecedorRequest(
        @NotBlank(message = "O nome do fornecedor é obrigatório") String nome,
        String contato,
        @Size(max = 32, message = "O CNPJ/documento deve ter no máximo 32 caracteres") String cnpj) {
}