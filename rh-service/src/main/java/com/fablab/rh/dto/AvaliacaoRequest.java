package com.fablab.rh.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Avaliação de um aluno em um treinamento (feita pelo tutor; nota 0 a 10).
 *
 * @param idFuncionario aluno avaliado
 * @param nota          nota atribuída (0-10)
 * @param feedback      comentários do tutor
 * @param dataAvaliacao data da avaliação (padrão: hoje)
 */
public record AvaliacaoRequest(
        @NotNull(message = "idFuncionario é obrigatório")
        Long idFuncionario,

        @NotNull(message = "nota é obrigatória")
        @DecimalMin(value = "0.0", message = "nota deve ser entre 0 e 10")
        @DecimalMax(value = "10.0", message = "nota deve ser entre 0 e 10")
        BigDecimal nota,

        String feedback,

        LocalDate dataAvaliacao) {
}