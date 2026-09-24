package com.fablab.notification.exception;

/**
 * Acesso a recurso de outro usuário (HTTP 403).
 */
public class ForbiddenException extends RuntimeException {

    public ForbiddenException(String message) {
        super(message);
    }
}
