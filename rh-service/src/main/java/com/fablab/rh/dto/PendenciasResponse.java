package com.fablab.rh.dto;

/**
 * Pendências do integrante (KPI do detalhe).
 *
 * @param horasPendentes apontamentos aguardando validação
 */
public record PendenciasResponse(
        long horasPendentes) {
}
