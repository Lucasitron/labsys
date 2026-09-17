package com.fablab.producao.dto;

import com.fablab.producao.entity.ProjetoStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

/** Dados de entrada para criação/atualização de projeto. */
public record ProjetoRequest(
        @NotBlank @Size(max = 150) String nome,
        @Size(max = 1000) String descricao,
        @NotNull LocalDate dataInicio,
        LocalDate dataFimPrevista,
        @NotNull Long idResponsavel,
        ProjetoStatus status) {
}