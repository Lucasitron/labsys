package com.fablab.estoque.exception;

/**
 * Lançada quando uma movimentação deixaria {@code quantidade_atual} negativa.
 *
 * <p>Mapeada para HTTP 409 (Conflito) no {@code GlobalExceptionHandler}.</p>
 */
public class SaldoInsuficienteException extends RuntimeException {

    public SaldoInsuficienteException(String message) {
        super(message);
    }
}