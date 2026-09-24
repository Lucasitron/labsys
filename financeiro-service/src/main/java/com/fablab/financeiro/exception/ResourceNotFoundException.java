package com.fablab.financeiro.exception;

/** Recurso não encontrado (→ 404 com mensagem em PT). */
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String message) {
        super(message);
    }
}
