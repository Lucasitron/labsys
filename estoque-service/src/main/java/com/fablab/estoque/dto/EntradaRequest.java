package com.fablab.estoque.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Requisição de entrada de estoque (compra simples).
 */
public record EntradaRequest(
        @NotNull(message = "O item é obrigatório") Long idItem,
        @NotNull(message = "O fornecedor é obrigatório") Long idFornecedor,
        @NotNull(message = "A quantidade é obrigatória")
        @DecimalMin(value = "0.01", message = "A quantidade deve ser maior que zero") BigDecimal quantidade,
        @NotNull(message = "O valor unitário é obrigatório")
        @DecimalMin(value = "0", message = "O valor unitário não pode ser negativo") BigDecimal valorUnitario,
        LocalDate dataEntrada,
        String notaFiscal,
        String observacao) {
}