package com.fablab.financeiro.service;

/**
 * Interface de integração com o processador de pagamentos.
 *
 * <p>Definida como contrato para futura integração (ex.: Pix, cartão de
 * crédito); sem implementação no MVP. O registro do pagamento continua de forma
 * manual via {@code PUT /lancamentos/{id}/pagamento}.</p>
 */
public interface PaymentProcessor {

    /**
     * Processa o pagamento/recebimento de um lançamento.
     *
     * @param idLancamento id do lançamento financeiro
     * @param valor valor do lançamento
     * @return resultado da operação de pagamento
     */
    PaymentResult processarPagamento(Long idLancamento, java.math.BigDecimal valor);

    /** Resultado de uma operação de pagamento. */
    record PaymentResult(boolean aprovado, String mensagem) {
    }
}