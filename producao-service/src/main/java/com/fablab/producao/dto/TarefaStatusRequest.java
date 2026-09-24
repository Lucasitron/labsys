package com.fablab.producao.dto;

import com.fablab.producao.entity.TarefaStatus;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

/** Alteração de status de uma tarefa. */
public record TarefaStatusRequest(
        @NotNull TarefaStatus status,
        LocalDate dataConclusao) {
}