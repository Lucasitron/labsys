package com.fablab.auth.exception;

/**
 * Lançada quando as credenciais de login são inválidas (HTTP 401).
 */
public class CredenciaisInvalidasException extends RuntimeException {

    public CredenciaisInvalidasException(String message) {
        super(message);
    }
}