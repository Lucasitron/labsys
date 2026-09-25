package com.fablab.financeiro.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Agendamento periódico da varredura de lançamentos vencidos (task-011).
 * Cron e habilitação configuráveis via propriedades.
 */
@Component
public class VencidosScheduler {

    private final LancamentoFinanceiroService lancamentoService;
    private final boolean enabled;

    public VencidosScheduler(LancamentoFinanceiroService lancamentoService,
                             @Value("${financeiro.vencidos.scheduler.enabled:true}") boolean enabled) {
        this.lancamentoService = lancamentoService;
        this.enabled = enabled;
    }

    @Scheduled(cron = "${financeiro.vencidos.scheduler.cron:0 0 8 * * *}")
    public void varrer() {
        if (!enabled) {
            return;
        }
        lancamentoService.emitirVencidos();
    }
}
