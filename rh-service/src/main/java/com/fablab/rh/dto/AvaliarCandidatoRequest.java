package com.fablab.rh.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

/**
 * Avaliação individual do candidato (pelo tutor responsável).
 *
 * @param nota     nota de 0 a 10
 * @param feedback comentários do tutor
 */
public record AvaliarCandidatoRequest(
        @NotNull(message = "nota é obrigatória")
        @DecimalMin(value = "0.0", message = "nota deve ser entre 0 e 10")
        @DecimalMax(value = "10.0", message = "nota deve ser entre 0 e 10")
        BigDecimal nota,

        String feedback) {
}
