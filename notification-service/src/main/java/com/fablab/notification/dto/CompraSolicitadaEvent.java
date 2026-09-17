package com.fablab.notification.dto;

/** Evento {@code compra.solicitada.event} consumido do Financeiro Service (opcional). */
public record CompraSolicitadaEvent(
        Long idCompra,
        Long idFornecedor) {
}
