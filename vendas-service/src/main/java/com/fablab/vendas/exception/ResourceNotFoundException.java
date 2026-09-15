package com.fablab.vendas.exception;

/**
 * Lançada quando um recurso não é encontrado.
 */
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String message) {
        super(message);
    }
}