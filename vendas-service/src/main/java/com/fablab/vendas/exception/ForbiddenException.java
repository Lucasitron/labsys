package com.fablab.vendas.exception;

/**
 * Lançada quando o usuário autenticado não tem permissão para a operação.
 */
public class ForbiddenException extends RuntimeException {

    public ForbiddenException(String message) {
        super(message);
    }
}