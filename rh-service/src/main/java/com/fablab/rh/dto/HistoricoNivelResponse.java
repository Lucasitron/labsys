package com.fablab.rh.dto;

import com.fablab.rh.entity.NivelAcesso;
import java.time.Instant;

/**
 * Item da timeline de evolução de nível (aba Histórico de nível do detalhe).
 *
 * @param nivelAntigo  nível anterior
 * @param rotuloAntigo rótulo do nível anterior
 * @param nivelNovo    nível novo
 * @param rotuloNovo   rótulo do nível novo
 * @param alteradoPor  nome do admin que alterou
 * @param dataAlteracao data da alteração
 */
public record HistoricoNivelResponse(
        NivelAcesso nivelAntigo,
        String rotuloAntigo,
        NivelAcesso nivelNovo,
        String rotuloNovo,
        String alteradoPor,
        Instant dataAlteracao) {
}
