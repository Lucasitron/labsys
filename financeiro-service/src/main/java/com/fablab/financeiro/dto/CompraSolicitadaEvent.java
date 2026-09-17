package com.fablab.financeiro.dto;

/**
 * Evento {@code compra.solicitada.event} publicado para o Estoque &amp;
 * Suprimentos Service como fluxo informativo de solicitação de compra.
 *
 * @param idCompra id da solicitação de compra registrada no Financeiro
 * @param idFornecedor fornecedor sugerido, quando informado (MVP: {@code null})
 */
public record CompraSolicitadaEvent(
        Long idCompra,
        Long idFornecedor) {
}