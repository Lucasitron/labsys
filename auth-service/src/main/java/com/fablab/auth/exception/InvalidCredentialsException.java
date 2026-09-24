package com.fablab.auth.exception;

/**
 * Lançada quando as credenciais de login são inválidas ou inexistentes.
 */
public class InvalidCredentialsException extends RuntimeException {

    public InvalidCredentialsException() {
        super("Credenciais inválidas");
    }
}