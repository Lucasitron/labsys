package com.fablab.notification.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Evento {@code encomenda.criada.event} consumido do Vendas Service. */
public record EncomendaCriadaEvent(
        Long idEncomenda,
        Long idCliente,
        String statusKanban,
        BigDecimal valorFinal,
        LocalDate dataCriacao) {
}
