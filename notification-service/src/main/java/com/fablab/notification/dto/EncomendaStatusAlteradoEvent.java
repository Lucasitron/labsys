package com.fablab.notification.dto;

import java.time.LocalDateTime;

/** Evento {@code encomenda.status.alterado.event} consumido do Vendas Service. */
public record EncomendaStatusAlteradoEvent(
        Long idEncomenda,
        Long idCliente,
        String statusAnterior,
        String statusNovo,
        LocalDateTime dataAlteracao) {
}
