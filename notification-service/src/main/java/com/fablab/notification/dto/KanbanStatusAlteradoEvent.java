package com.fablab.notification.dto;

import java.time.LocalDateTime;

/** Evento {@code kanban.status.alterado.event} consumido do Produção Service. */
public record KanbanStatusAlteradoEvent(
        Long idEncomenda,
        String statusAnterior,
        String statusNovo,
        LocalDateTime dataAlteracao) {
}
