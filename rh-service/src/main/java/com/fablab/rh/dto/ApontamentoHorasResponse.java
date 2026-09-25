package com.fablab.rh.dto;

import com.fablab.rh.entity.StatusApontamento;
import com.fablab.rh.entity.TipoApontamento;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;

/**
 * Resposta com os dados de um apontamento de horas.
 */
public record ApontamentoHorasResponse(
        Long id,
        Long idFuncionario,
        TipoApontamento tipo,
        Long idReferencia,
        LocalDate data,
        BigDecimal horasTrabalhadas,
        LocalTime horaInicio,
        LocalTime horaFim,
        String descricaoAtividade,
        StatusApontamento status,
        String motivoRejeicao,
        Long idAdminValidador,
        Instant dataValidacao) {
}
