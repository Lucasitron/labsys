package com.fablab.vendas.exception;

/** Conflito de regra de negócio ou concorrência (HTTP 409). */
public class ConflitoException extends RuntimeException {

    public ConflitoException(String message) {
        super(message);
    }
}
