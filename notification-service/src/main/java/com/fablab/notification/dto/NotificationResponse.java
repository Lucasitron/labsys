package com.fablab.notification.dto;

import java.time.Instant;

/**
 * Notificação no contrato do frontend (§6.3 D-1).
 *
 * @param id        id da notificação
 * @param type      tipo (encomenda|estoque|financeiro|producao|pessoas|sistema)
 * @param title     título
 * @param body      corpo (opcional)
 * @param read      se já foi lida
 * @param createdAt instante de criação
 * @param link      destino opcional (caminho interno ou URL http(s))
 * @param channel   canal (inapp|email|push)
 */
public record NotificationResponse(
        Long id,
        String type,
        String title,
        String body,
        boolean read,
        Instant createdAt,
        String link,
        String channel) {
}
