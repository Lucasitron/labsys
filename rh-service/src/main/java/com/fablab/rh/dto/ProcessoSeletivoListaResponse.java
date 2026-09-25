package com.fablab.rh.dto;

import java.util.List;

/**
 * Kanban do processo seletivo: grupos com candidatos e contagens.
 *
 * @param grupos grupos com seus candidatos
 * @param totais contagens do funil
 */
public record ProcessoSeletivoListaResponse(
        List<GrupoProcessoResponse> grupos,
        TotaisProcessoResponse totais) {
}
