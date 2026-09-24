package com.fablab.auth.dto;

import java.time.Instant;

/**
 * Resultado da validação de RFID. Para cartões desconhecidos {@code allowed} é
 * {@code false} e os dados do usuário ficam {@code null}.
 */
public record ValidateRfidResponse(
        boolean allowed,
        String message,
        Long idUser,
        String uuidRfid,
        String nomeUsuario,
        String setor,
        String type,
        Instant timestamp) {
}