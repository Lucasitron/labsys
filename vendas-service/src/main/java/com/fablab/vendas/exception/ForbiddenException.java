package com.fablab.vendas.exception;

/** Operação negada ao usuário autenticado (HTTP 403). */
public class ForbiddenException extends RuntimeException {

    public ForbiddenException(String message) {
        super(message);
    }
}
