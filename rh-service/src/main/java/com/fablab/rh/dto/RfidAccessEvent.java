package com.fablab.rh.dto;

import java.time.Instant;

/**
 * Evento assíncrono de acesso RFID publicado pelo Auth &amp; Identity Service
 * na exchange {@code fablab.access}, routing key {@code access.rfid.event}.
 *
 * @param idUser       id externo do usuário (equivale a {@code pessoa.id})
 * @param uuidRfid     identificador do cartão RFID
 * @param timestamp    data/hora da leitura
 * @param type         tipo do acesso (ENTRADA, SAIDA, ACESSO_NEGADO)
 */
public record RfidAccessEvent(
        Long idUser,
        String uuidRfid,
        Instant timestamp,
        String type) {
}