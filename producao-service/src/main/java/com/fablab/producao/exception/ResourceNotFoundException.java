package com.fablab.producao.exception;

/** Lançada quando um recurso solicitado não existe (HTTP 404). */
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String message) {
        super(message);
    }

    public ResourceNotFoundException(String recurso, Long id) {
        super(recurso + " não encontrado(a) com id " + id);
    }
}