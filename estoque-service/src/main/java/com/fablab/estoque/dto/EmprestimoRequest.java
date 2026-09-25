package com.fablab.estoque.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Requisição de registro de empréstimo.
 */
public record EmprestimoRequest(
        @NotNull(message = "O item é obrigatório") Long idItem,
        @NotNull(message = "A pessoa é obrigatória") Long idPessoa,
        @NotNull(message = "A quantidade é obrigatória")
        @DecimalMin(value = "0.01", message = "A quantidade deve ser maior que zero") BigDecimal quantidade,
        @NotNull(message = "A data de devolução prevista é obrigatória")
        @FutureOrPresent(message = "A data de devolução prevista deve ser hoje ou futura") LocalDate dataDevolucaoPrevista,
        String observacao) {
}