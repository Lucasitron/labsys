package com.fablab.rh.dto;

import com.fablab.rh.entity.StatusProcesso;
import java.math.BigDecimal;

/**
 * Candidato dentro de um grupo do processo seletivo.
 *
 * @param id       id da pessoa candidata
 * @param nome     nome da pessoa candidata
 * @param etapa    etapa individual do candidato
 * @param nota     nota da avaliação individual (nula quando não avaliado)
 * @param feedback feedback da avaliação individual
 */
public record CandidatoGrupoResponse(
        Long id,
        String nome,
        StatusProcesso etapa,
        BigDecimal nota,
        String feedback) {
}
