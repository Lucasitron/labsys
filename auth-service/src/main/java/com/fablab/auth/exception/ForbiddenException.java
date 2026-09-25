package com.fablab.auth.exception;

/**
 * Lançada quando o usuário não possui permissão para a ação.
 */
public class ForbiddenException extends RuntimeException {

    public ForbiddenException(String message) {
        super(message);
    }
}