package com.fablab.notification.dto;

import java.util.List;

/**
 * Lista paginada canônica de notificações (D-1: {@code items/total/page/size}).
 *
 * <p>{@code pageSize} e {@code totalPages} são aliases de compatibilidade com
 * o paginador do frontend.
 */
public record PagedNotifications(
        List<NotificationResponse> items,
        long total,
        int page,
        int size,
        int pageSize,
        int totalPages) {
}
