package com.fablab.notification.dto;

import java.time.Instant;

/**
 * Notificação exibida na lista do usuário.
 *
 * @param id      id da notificação
 * @param titulo  título
 * @param mensagem mensagem (opcional)
 * @param lida    se já foi lida
 * @param criadaEm instante de criação
 */
public record NotificacaoResponse(Long id, String titulo, String mensagem, boolean lida, Instant criadaEm) {
}