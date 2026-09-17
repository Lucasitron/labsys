package com.fablab.financeiro.entity;

/**
 * Situação de um fechamento de encomenda.
 *
 * <p>Valores congelados a partir da criação em {@code ABERTA}: após o
 * fechamento, {@code horasEstimadas} e {@code valorFechado} não podem ser
 * alterados — uma mudança de escopo exigirá uma nova encomenda.</p>
 */
public enum StatusFechamentoEncomenda {
    ABERTA,
    CONCLUIDA,
    CANCELADA
}