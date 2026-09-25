package com.fablab.producao.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Dados de entrada para atualizar um parâmetro 5S. */
public record Parametro5SRequest(
        @NotBlank @Size(max = 255) String valor,
        @Size(max = 500) String descricao) {
}