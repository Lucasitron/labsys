package com.fablab.rh.dto;

import com.fablab.rh.entity.NivelAcesso;
import jakarta.validation.constraints.NotNull;

/**
 * Alteração do nível de um membro (Admin).
 *
 * @param nivel novo nível de acesso
 */
public record NivelMembroRequest(
        @NotNull(message = "nivel é obrigatório")
        NivelAcesso nivel) {
}
