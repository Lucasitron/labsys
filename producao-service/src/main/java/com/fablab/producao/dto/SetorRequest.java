package com.fablab.producao.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Dados de entrada para criação/atualização de setor 5S. */
public record SetorRequest(
        @NotNull Integer numero,
        @NotBlank @Size(max = 150) String nome,
        @Size(max = 1000) String descricao,
        @Size(max = 1000) String observacoes,
        @Size(max = 500) String fotoCorretoUrl,
        @Size(max = 500) String fotoIncorretoUrl,
        Boolean ativo) {
}