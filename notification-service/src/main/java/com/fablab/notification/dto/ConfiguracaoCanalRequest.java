package com.fablab.notification.dto;

import jakarta.validation.constraints.NotNull;

/** Atualização da configuração de um canal. */
public record ConfiguracaoCanalRequest(
        @NotNull Boolean habilitado,
        String parametros) {
}
