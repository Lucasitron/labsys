package com.fablab.rh.dto;

import com.fablab.rh.entity.NivelAcesso;
import jakarta.validation.constraints.NotNull;

/**
 * Solicitação de alteração de nível de acesso (exclusivo para Admin).
 *
 * @param nivelNovo novo nível de acesso (0-4)
 */
public record NivelRequest(
        @NotNull(message = "nivelNovo é obrigatório")
        NivelAcesso nivelNovo) {
}