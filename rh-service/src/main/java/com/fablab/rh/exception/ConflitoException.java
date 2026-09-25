package com.fablab.rh.exception;

/**
 * Conflito de negócio (HTTP 409) — ex.: exclusão com vínculos ativos.
 */
public class ConflitoException extends RuntimeException {

    public ConflitoException(String message) {
        super(message);
    }
}
