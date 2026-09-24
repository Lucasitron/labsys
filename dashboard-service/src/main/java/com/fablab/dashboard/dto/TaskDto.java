package com.fablab.dashboard.dto;

/**
 * Tarefa exibida no cartão "Minhas tarefas".
 *
 * @param id       identificador usado no {@code PATCH /tasks/{id}}
 * @param title    título/tarefa acionável
 * @param module   módulo dono (ex.: {@code rh})
 * @param due      data de vencimento (ISO) ou nulo
 * @param dueLabel rótulo pronto ({@code Hoje}, {@code dd/MM})
 * @param urgent   se vence hoje
 */
public record TaskDto(String id, String title, String module, String due, String dueLabel, Boolean urgent) {
}