package com.fablab.notification.dto;

import com.fablab.notification.entity.CanalNotificacao;

/** Envio de uma notificação de teste pelo Admin. */
public record NotificacaoTesteRequest(
        Long idDestinatario,
        CanalNotificacao canal,
        String assunto,
        String mensagem) {
}
