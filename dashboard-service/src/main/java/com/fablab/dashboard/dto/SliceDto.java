package com.fablab.dashboard.dto;

/**
 * Fatia de um gráfico de status (donut).
 *
 * @param status identificador canônico ({@code in_production}, {@code active}…)
 * @param label  rótulo exibido servido pronto
 * @param count  quantidade
 * @param color  cor do segmento ({@code brand|warn|success|danger|muted|neutral|gray})
 */
public record SliceDto(String status, String label, Integer count, String color) {
}