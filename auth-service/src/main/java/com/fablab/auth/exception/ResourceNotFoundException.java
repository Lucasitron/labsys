package com.fablab.auth.exception;

/**
 * Lançada quando um recurso não é encontrado (HTTP 404).
 */
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String message) {
        super(message);
    }
}