package com.fablab.financeiro.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Payload de definição do valor da hora por nível de acesso.
 */
public record ValorHoraNivelRequest(
        @NotNull @Min(0) @Max(3) Integer nivelAcesso,
        @NotNull @Positive BigDecimal valorHora,
        @NotNull LocalDate dataVigencia) {
}