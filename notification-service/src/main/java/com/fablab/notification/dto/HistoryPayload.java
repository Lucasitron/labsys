package com.fablab.notification.dto;

import java.util.List;

/**
 * Histórico completo de notificações (D-7: {@code items/total}).
 *
 * <p>O frontend pagina client-side; o backend sempre retorna a lista
 * integral (D-4).
 */
public record HistoryPayload(List<NotificationResponse> items, long total) {
}
