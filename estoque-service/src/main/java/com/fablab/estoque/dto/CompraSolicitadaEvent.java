package com.fablab.estoque.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Evento {@code compra.solicitada.event}: entrada de estoque registrada e
 * encaminhada ao Financeiro (contas a pagar) e ao Notification.
 */
public record CompraSolicitadaEvent(
        Long idEntrada,
        Long idItem,
        Long idFornecedor,
        BigDecimal quantidade,
        BigDecimal valorUnitario,
        BigDecimal valorTotal,
        LocalDate dataEntrada,
        String notaFiscal) {
}