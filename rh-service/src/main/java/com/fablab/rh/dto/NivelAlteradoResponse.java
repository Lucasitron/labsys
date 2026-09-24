package com.fablab.rh.dto;

import com.fablab.rh.entity.NivelAcesso;
import java.time.Instant;

/**
 * Resposta da alteração de nível de acesso.
 */
public record NivelAlteradoResponse(
        Long idFuncionario,
        NivelAcesso nivelAntigo,
        NivelAcesso nivelNovo,
        Instant dataAlteracao) {
}