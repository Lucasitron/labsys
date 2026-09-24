package com.fablab.auth.exception;

/**
 * Lançada quando um token JWT foi revogado (presente na blacklist).
 */
public class TokenBlacklistedException extends RuntimeException {

    public TokenBlacklistedException() {
        super("Token revogado");
    }
}