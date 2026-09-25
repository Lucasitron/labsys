package com.fablab.rh.dto;

import com.fablab.rh.entity.StatusProcesso;
import java.util.List;

/**
 * Grupo do processo seletivo com seus candidatos.
 *
 * @param id            id do grupo (nulo para "Sem grupo")
 * @param nome          nome do grupo
 * @param etapa         etapa predominante do grupo
 * @param totalMembros  quantidade de candidatos no grupo
 * @param idLider       id do funcionário tutor líder (nulo para "Sem grupo")
 * @param nomeLider     nome do líder (nulo para "Sem grupo")
 * @param candidatos    candidatos do grupo com etapa individual
 */
public record GrupoProcessoResponse(
        Long id,
        String nome,
        StatusProcesso etapa,
        int totalMembros,
        Long idLider,
        String nomeLider,
        List<CandidatoGrupoResponse> candidatos) {
}
