package com.fablab.producao.dto;

import com.fablab.producao.entity.KanbanStatus;
import java.time.LocalDateTime;

/**
 * Evento {@code kanban.status.alterado.event} publicado quando um cartão muda de
 * coluna, para o Vendas e o Notification.
 */
public record KanbanStatusAlteradoEvent(
        Long idEncomenda,
        KanbanStatus statusAnterior,
        KanbanStatus statusNovo,
        LocalDateTime dataAlteracao) {
}