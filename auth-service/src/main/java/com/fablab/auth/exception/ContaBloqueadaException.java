package com.fablab.auth.exception;

/**
 * Lançada quando o usuário existe porém está bloqueado/desativado (HTTP 423).
 */
public class ContaBloqueadaException extends RuntimeException {

    public ContaBloqueadaException(String message) {
        super(message);
    }
}