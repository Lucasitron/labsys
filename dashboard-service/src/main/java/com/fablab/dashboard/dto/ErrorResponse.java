package com.fablab.dashboard.dto;

import java.time.Instant;
import java.util.Map;

/**
 * Corpo padrão de erro retornado pelo {@code GlobalExceptionHandler} da malha.
 */
public record ErrorResponse(
        Instant timestamp,
        int status,
        String error,
        String message,
        String path,
        Map<String, String> fieldErrors) {

    public static ErrorResponse of(org.springframework.http.HttpStatus status, String message, String path) {
        return new ErrorResponse(Instant.now(), status.value(), status.getReasonPhrase(), message, path, Map.of());
    }
}