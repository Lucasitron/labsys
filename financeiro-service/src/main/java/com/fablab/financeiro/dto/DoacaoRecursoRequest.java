package com.fablab.financeiro.dto;

import com.fablab.financeiro.entity.TipoDoacaoRecurso;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Payload de registro de doação ou recurso de projeto.
 */
public record DoacaoRecursoRequest(
        @NotNull TipoDoacaoRecurso tipo,
        @NotBlank @Size(max = 150) String origem,
        @NotNull @Positive BigDecimal valor,
        @NotNull LocalDate dataRecebimento,
        Long idProjetoAssociado) {
}