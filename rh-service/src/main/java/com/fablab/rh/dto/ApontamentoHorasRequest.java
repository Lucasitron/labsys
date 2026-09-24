package com.fablab.rh.dto;

import com.fablab.rh.entity.TipoApontamento;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

/**
 * Registro de apontamento de horas em encomenda ou projeto.
 *
 * <p>Quando {@code horaInicio} e {@code horaFim} são informados, as
 * {@code horasTrabalhadas} são recalculadas no servidor e o valor enviado é
 * ignorado.</p>
 *
 * @param idFuncionario     funcionário que realizou o apontamento
 * @param tipo              ENCOMENDA ou PROJETO
 * @param idReferencia      ID da encomenda ou do projeto
 * @param data              data da atividade
 * @param horasTrabalhadas  horas dedicadas (obrigatório quando sem início/fim)
 * @param descricaoAtividade descrição do que foi feito
 * @param horaInicio        início da atividade (ex.: {@code 14:00})
 * @param horaFim           fim da atividade (ex.: {@code 17:00})
 */
public record ApontamentoHorasRequest(
        @NotNull(message = "idFuncionario é obrigatório")
        Long idFuncionario,

        @NotNull(message = "tipo é obrigatório")
        TipoApontamento tipo,

        @NotNull(message = "idReferencia é obrigatório")
        Long idReferencia,

        @NotNull(message = "data é obrigatória")
        LocalDate data,

        @DecimalMin(value = "0.01", message = "horasTrabalhadas deve ser maior que zero")
        @DecimalMax(value = "99.99", message = "horasTrabalhadas deve ser no máximo 99,99")
        BigDecimal horasTrabalhadas,

        String descricaoAtividade,

        LocalTime horaInicio,

        LocalTime horaFim) {
}
