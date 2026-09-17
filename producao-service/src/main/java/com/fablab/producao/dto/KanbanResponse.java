package com.fablab.producao.dto;

import com.fablab.producao.entity.EncomendaKanban;
import com.fablab.producao.entity.KanbanStatus;
import java.time.LocalDateTime;

/** Representação de um cartão do Kanban. */
public record KanbanResponse(
        Long idKanban,
        Long idEncomenda,
        KanbanStatus status,
        LocalDateTime dataEntradaStatus,
        Long idResponsavel,
        Integer ordem,
        Long version) {

    public static KanbanResponse from(EncomendaKanban kanban) {
        return new KanbanResponse(
                kanban.getIdKanban(),
                kanban.getIdEncomenda(),
                kanban.getStatus(),
                kanban.getDataEntradaStatus(),
                kanban.getIdResponsavel(),
                kanban.getOrdem(),
                kanban.getVersion());
    }
}