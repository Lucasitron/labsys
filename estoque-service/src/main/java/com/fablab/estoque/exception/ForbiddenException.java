package com.fablab.estoque.exception;

/**
 * Exceção lançada quando o usuário autenticado não possui permissão para
 * acessar ou alterar o recurso.
 */
public class ForbiddenException extends RuntimeException {

    public ForbiddenException(String message) {
        super(message);
    }
}