package com.fablab.producao.dto;

import com.fablab.producao.entity.PrioridadeTarefa;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

/** Dados de entrada para criação/atualização de tarefa. */
public record TarefaRequest(
        @NotNull Long idProjeto,
        @NotBlank @Size(max = 150) String titulo,
        @Size(max = 1000) String descricao,
        Long idResponsavel,
        LocalDate dataInicio,
        LocalDate dataFimPrevista,
        PrioridadeTarefa prioridade) {
}