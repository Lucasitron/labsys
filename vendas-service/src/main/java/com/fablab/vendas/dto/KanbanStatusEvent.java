package com.fablab.vendas.dto;

import java.time.LocalDateTime;

/**
 * Evento {@code kanban.status.event} publicado para o Notification Service.
 */
public record KanbanStatusEvent(
        Long idEncomenda,
        Long idCliente,
        String statusAnterior,
        String statusNovo,
        LocalDateTime dataAlteracao) {
}