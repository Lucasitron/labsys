package com.fablab.rh.dto;

import com.fablab.rh.entity.StatusApontamento;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Validação ou rejeição de um apontamento (exclusivo para Admin).
 *
 * @param status VALIDADO ou REJEITADO
 * @param motivo motivo da rejeição (opcional; ignorado na validação)
 */
public record ValidacaoApontamentoRequest(
        @NotNull(message = "status é obrigatório")
        StatusApontamento status,

        @Size(max = 500, message = "motivo deve ter no máximo 500 caracteres")
        String motivo) {
}
