package com.fablab.producao.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

/** Dados de entrada para registrar um projeto de mesa. */
public record ProjetoMesaRequest(
        @NotNull Long idFuncionario,
        @NotNull Long idMesa,
        @NotBlank @Size(max = 150) String nomeProjeto,
        @Size(max = 100) String tipoProjeto,
        LocalDate prazoExecucao) {
}