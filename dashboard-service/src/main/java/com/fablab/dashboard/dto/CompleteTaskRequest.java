package com.fablab.dashboard.dto;

import jakarta.validation.constraints.NotNull;

/**
 * Corpo do {@code PATCH /tasks/{id}}.
 *
 * @param done {@code true} marca a tarefa como concluída
 */
public record CompleteTaskRequest(@NotNull(message = "Informe o campo 'done'") Boolean done) {
}