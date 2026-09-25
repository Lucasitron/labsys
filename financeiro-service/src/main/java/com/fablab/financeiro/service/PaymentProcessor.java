package com.fablab.financeiro.service;

/**
 * Abstração do processamento de pagamentos (F9). No MVP o registro é 100%
 * manual; esta interface isola o núcleo financeiro para plugar Pix/Cartão/
 * Boleto e conciliação bancária num futuro {@code Payment Service} sem
 * refatorar o núcleo.
 */
public interface PaymentProcessor {

    /**
     * Registra um pagamento/recebimento manual de um lançamento.
     *
     * @param idLancamento id do lançamento liquidado
     * @param observacao   observação informativa do pagamento
     */
    void registrarPagamentoManual(Long idLancamento, String observacao);

    /** Nome do processador (ex.: {@code "manual"} no MVP). */
    default String nome() {
        return "manual";
    }
}
