package com.fablab.producao.dto;

import com.fablab.producao.entity.KanbanStatus;
import jakarta.validation.constraints.NotNull;

/** Movimentação de um cartão do Kanban. */
public record KanbanMovimentoRequest(
        @NotNull KanbanStatus statusNovo,
        Long idUsuario,
        String observacao) {
}