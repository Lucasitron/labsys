package com.fablab.estoque.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Resposta de entrada de estoque.
 */
public record EntradaResponse(
        Long id,
        Long idItem,
        String nomeItem,
        Long idFornecedor,
        String nomeFornecedor,
        BigDecimal quantidade,
        BigDecimal valorUnitario,
        BigDecimal valorTotal,
        LocalDate dataEntrada,
        String notaFiscal,
        String observacao,
        String responsavel) {
}