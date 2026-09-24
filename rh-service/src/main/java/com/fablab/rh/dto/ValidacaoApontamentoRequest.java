package com.fablab.rh.dto;

import com.fablab.rh.entity.StatusApontamento;
import jakarta.validation.constraints.NotNull;

/**
 * Validação ou rejeição de um apontamento (exclusivo para Admin).
 *
 * @param status VALIDADO ou REJEITADO
 */
public record ValidacaoApontamentoRequest(
        @NotNull(message = "status é obrigatório")
        StatusApontamento status) {
}