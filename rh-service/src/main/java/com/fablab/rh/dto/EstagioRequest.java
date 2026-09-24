package com.fablab.rh.dto;

import com.fablab.rh.entity.StatusProcesso;
import jakarta.validation.constraints.NotNull;

/**
 * Movimento de etapa do candidato no funil.
 *
 * @param etapa nova etapa (inscrito/triagem/entrevista/aprovado/reprovado)
 */
public record EstagioRequest(
        @NotNull(message = "etapa é obrigatória")
        StatusProcesso etapa) {
}
