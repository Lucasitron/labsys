package com.fablab.auth.exception;

/**
 * Lançada quando parâmetros do sistema ou dados do token são inválidos.
 * Responde HTTP 422 com mensagem PT não-técnica.
 */
public class ConfiguracaoInvalidaException extends RuntimeException {

    public ConfiguracaoInvalidaException(String message) {
        super(message);
    }
}
