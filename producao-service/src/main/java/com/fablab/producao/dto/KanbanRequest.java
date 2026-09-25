package com.fablab.producao.dto;

import jakarta.validation.constraints.NotNull;

/** Dados de entrada para inclusão de um cartão no Kanban. */
public record KanbanRequest(
        @NotNull Long idEncomenda,
        Long idResponsavel,
        Integer ordem) {
}