package com.fablab.notification.dto;

/**
 * Quantidade de notificações não lidas do usuário autenticado.
 *
 * @param count total de não lidas
 */
public record UnreadCountResponse(long count) {
}