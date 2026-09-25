package com.fablab.financeiro.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Implementação manual do {@link PaymentProcessor} (MVP): apenas registra em
 * log/auditoria, sem provedor concreto acoplado.
 */
@Component
public class ManualPaymentProcessor implements PaymentProcessor {

    private static final Logger log = LoggerFactory.getLogger(ManualPaymentProcessor.class);

    @Override
    public void registrarPagamentoManual(Long idLancamento, String observacao) {
        log.info("Pagamento manual registrado para o lançamento {}: {}",
                idLancamento, observacao != null ? observacao : "sem observação");
    }
}
