package com.fablab.rh.dto;

import com.fablab.rh.entity.NivelAcesso;
import java.time.Instant;

/**
 * Evento emitido quando o nível de acesso de um funcionário é alterado.
 *
 * @param idFuncionario id do funcionário alterado
 * @param nivelAntigo   nível anterior
 * @param nivelNovo     novo nível
 * @param data          data da alteração
 */
public record NivelAlteradoEvent(
        Long idFuncionario,
        NivelAcesso nivelAntigo,
        NivelAcesso nivelNovo,
        Instant data) {
}