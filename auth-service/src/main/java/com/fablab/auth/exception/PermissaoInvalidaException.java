package com.fablab.auth.exception;

/**
 * Lançada quando módulo, nível ou valor da matriz RBAC é inválido.
 * Responde HTTP 422 com mensagem PT não-técnica (C-3/C-4).
 */
public class PermissaoInvalidaException extends RuntimeException {

    public PermissaoInvalidaException(String message) {
        super(message);
    }
}
