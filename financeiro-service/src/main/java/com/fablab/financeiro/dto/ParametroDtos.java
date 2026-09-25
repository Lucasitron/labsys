package com.fablab.financeiro.dto;

import com.fablab.financeiro.entity.ParametroOverhead;
import com.fablab.financeiro.entity.ValorHoraNivel;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;

/** DTOs de valores/hora por nível e taxa de overhead. */
public final class ParametroDtos {

    private ParametroDtos() {
    }

    public record ValorHoraRequest(
            @NotNull(message = "O nível de acesso é obrigatório (0 a 3)")
            @Min(value = 0, message = "O nível deve ser entre 0 e 3")
            @Max(value = 3, message = "O nível deve ser entre 0 e 3")
            Integer nivelAcesso,
            @NotNull(message = "O valor/hora é obrigatório")
            @DecimalMin(value = "0.00", message = "O valor/hora não pode ser negativo")
            BigDecimal valorHora,
            @NotNull(message = "A data de vigência é obrigatória")
            LocalDate dataVigencia) {
    }

    public record ValorHoraResponse(
            Long id,
            Integer nivelAcesso,
            BigDecimal valorHora,
            LocalDate dataVigencia) {
        public static ValorHoraResponse of(ValorHoraNivel entity) {
            return new ValorHoraResponse(entity.getId(), entity.getNivelAcesso(),
                    entity.getValorHora(), entity.getDataVigencia());
        }
    }

    public record OverheadRequest(
            @NotNull(message = "A taxa de overhead é obrigatória")
            @DecimalMin(value = "0.00", message = "A taxa não pode ser negativa")
            BigDecimal valorTaxaHora,
            @NotNull(message = "A data de vigência é obrigatória")
            LocalDate dataVigencia) {
    }

    public record OverheadResponse(
            Long id,
            BigDecimal valorTaxaHora,
            LocalDate dataVigencia) {
        public static OverheadResponse of(ParametroOverhead entity) {
            return new OverheadResponse(entity.getId(), entity.getValorTaxaHora(), entity.getDataVigencia());
        }
    }
}
