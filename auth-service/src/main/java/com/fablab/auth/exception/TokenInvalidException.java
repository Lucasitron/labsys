package com.fablab.auth.exception;

/**
 * Lançada quando um token JWT é inválido, expirado ou malformado.
 */
public class TokenInvalidException extends RuntimeException {

    public TokenInvalidException() {
        super("Token inválido ou expirado");
    }
}