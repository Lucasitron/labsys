package com.fablab.producao.dto;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

/** Responsável por um setor 5S. */
public record ResponsavelSetorRequest(
        @NotNull Long idFuncionario,
        @NotNull LocalDate dataInicio,
        LocalDate dataFim,
        Boolean ativo) {
}