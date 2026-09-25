package com.fablab.dashboard.dto;

import java.util.List;
import java.util.Map;

/**
 * Resposta do {@code GET /dashboard/summary}. Campos de derivação visual
 * ({@code delta}, {@code deltaTone}, {@code dueLabel}, {@code urgent},
 * {@code color}) são servidos prontos pelo backend.
 *
 * @param tasks            tarefas pendentes do usuário no App Shell
 * @param kpis             indicadores-chave por card
 * @param ordersByStatus   fatia de encomendas por status (Vendas/Produção)
 * @param machinesByStatus fatia de máquinas por status (Produção)
 * @param activity         atividade recente (timeline do dashboard)
 */
public record DashboardSummary(
        List<TaskDto> tasks,
        Map<String, KpiDto> kpis,
        List<SliceDto> ordersByStatus,
        List<SliceDto> machinesByStatus,
        List<ActivityItemDto> activity) {
}