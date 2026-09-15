package com.fablab.vendas.dto;

import com.fablab.vendas.entity.StatusKanban;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Payload de movimentação de encomenda no Kanban.
 */
public record KanbanRequest(
        @NotNull StatusKanban novoStatus,
        Long idUsuario,
        @Size(max = 500) String observacao) {
}