package com.fablab.rh.dto;

/**
 * Contagens do funil do processo seletivo.
 *
 * @param inscritos   total de candidatos com processo aberto
 * @param grupos      total de grupos
 * @param emAvaliacao candidatos em triagem ou entrevista
 * @param aprovados   candidatos aprovados
 */
public record TotaisProcessoResponse(
        long inscritos,
        long grupos,
        long emAvaliacao,
        long aprovados) {
}
