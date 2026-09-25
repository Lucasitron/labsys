package com.fablab.rh.dto;

/**
 * Apontamentos por status (aba Horas do detalhe).
 *
 * @param pendentes apontamentos aguardando validação
 * @param validadas apontamentos validados
 * @param rejeitadas apontamentos rejeitados
 */
public record HorasPorStatusResponse(
        long pendentes,
        long validadas,
        long rejeitadas) {
}
