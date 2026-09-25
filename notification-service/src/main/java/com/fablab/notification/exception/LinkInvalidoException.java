package com.fablab.notification.exception;

/**
 * Link de notificação fora do formato/allowlist (HTTP 422).
 */
public class LinkInvalidoException extends RuntimeException {

    public LinkInvalidoException(String message) {
        super(message);
    }
}
