package com.fablab.vendas.dto;

import java.time.LocalDateTime;

/**
 * Evento {@code encomenda.status.alterado.event} publicado para o Notification
 * Service.
 */
public record KanbanStatusEvent(
        Long idEncomenda,
        Long idCliente,
        String statusAnterior,
        String statusNovo,
        LocalDateTime dataAlteracao) {
}