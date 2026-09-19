package com.fablab.dashboard.dto;

/**
 * Card de indicador do dashboard.
 *
 * @param value     valor principal exibido
 * @param total     total quando aplicável (ex.: máquinas ativas "de X")
 * @param delta     rótulo de variação servido pronto
 * @param deltaTone tom do delta ({@code success|danger|warn|muted})
 * @param restricted {@code true} se o valor é restrito a responsáveis
 */
public record KpiDto(Integer value, Integer total, String delta, String deltaTone, Boolean restricted) {
}