package com.fablab.financeiro.exception;

/**
 * Lançada quando o usuário autenticado não tem permissão para a operação.
 */
public class ForbiddenException extends RuntimeException {

    public ForbiddenException(String message) {
        super(message);
    }
}