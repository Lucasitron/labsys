package com.fablab.notification.dto;

import jakarta.validation.constraints.NotEmpty;
import java.util.List;

/** Seleção de notificações ativas a mover para o histórico. */
public record RevisaoRequest(@NotEmpty List<Long> idsNotificacoes) {
}
