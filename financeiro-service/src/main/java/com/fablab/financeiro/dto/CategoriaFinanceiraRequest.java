package com.fablab.financeiro.dto;

import com.fablab.financeiro.entity.TipoCategoriaFinanceira;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Payload de criação de categoria financeira.
 */
public record CategoriaFinanceiraRequest(
        @NotBlank @Size(max = 100) String nome,
        @NotNull TipoCategoriaFinanceira tipo,
        @Size(max = 255) String descricao) {
}