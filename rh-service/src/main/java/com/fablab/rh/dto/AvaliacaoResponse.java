package com.fablab.rh.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Resposta com uma avaliação de treinamento.
 */
public record AvaliacaoResponse(
        Long id,
        Long idTreinamento,
        Long idFuncionario,
        String nomeAluno,
        BigDecimal nota,
        String feedback,
        LocalDate dataAvaliacao) {
}