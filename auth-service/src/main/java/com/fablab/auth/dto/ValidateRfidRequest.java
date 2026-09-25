package com.fablab.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Requisição de validação de acesso físico via RFID (ESP32).
 */
public record ValidateRfidRequest(
        @NotBlank(message = "uuid é obrigatório")
        @Size(max = 64, message = "uuid deve ter no máximo 64 caracteres")
        String uuid) {
}