package com.fablab.rh.dto;

import jakarta.validation.constraints.Size;

/**
 * Corpo opcional da rejeição de horas.
 *
 * @param motivo justificativa da rejeição
 */
public record RejeicaoApontamentoRequest(
        @Size(max = 500, message = "motivo deve ter no máximo 500 caracteres")
        String motivo) {
}
