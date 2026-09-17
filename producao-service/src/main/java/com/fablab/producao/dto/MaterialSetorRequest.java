package com.fablab.producao.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

/** Item de material de um setor 5S. */
public record MaterialSetorRequest(
        @NotBlank @Size(max = 255) String descricao,
        @NotNull @PositiveOrZero BigDecimal quantidade) {
}