package com.fablab.auth.dto;

import com.fablab.auth.entity.AccessLogType;
import java.time.Instant;

/**
 * Evento assíncrono de acesso RFID publicado no RabbitMQ (exchange
 * {@code fablab.access}, routing key {@code access.rfid.event}).
 *
 * <p>Consumido pelo Pessoas &amp; RH Service para registro de ponto.</p>
 */
public record RfidAccessEvent(
        Long idUser,
        String uuidRfid,
        Instant timestamp,
        AccessLogType type) {
}