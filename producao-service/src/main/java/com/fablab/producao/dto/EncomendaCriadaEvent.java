package com.fablab.producao.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Evento {@code encomenda.criada.event} publicado pelo Vendas. */
public record EncomendaCriadaEvent(
        Long idEncomenda,
        Long idCliente,
        String statusKanban,
        BigDecimal valorFinal,
        LocalDate dataCriacao) {
}