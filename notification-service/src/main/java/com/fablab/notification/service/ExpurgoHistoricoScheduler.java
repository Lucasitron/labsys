package com.fablab.notification.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Job diário de expurgo: remove definitivamente os históricos revisados há mais
 * que o período de retenção configurado (padrão 90 dias).
 */
@Component
@ConditionalOnProperty(name = "notification.expurgo.scheduler.enabled", havingValue = "true",
        matchIfMissing = true)
public class ExpurgoHistoricoScheduler {

    private static final Logger log = LoggerFactory.getLogger(ExpurgoHistoricoScheduler.class);

    private final NotificacaoService notificacaoService;

    public ExpurgoHistoricoScheduler(NotificacaoService notificacaoService) {
        this.notificacaoService = notificacaoService;
    }

    /** Executa diariamente às 3h. */
    @Scheduled(cron = "${notification.expurgo.scheduler.cron:0 0 3 * * *}")
    public void expurgar() {
        int removidos = notificacaoService.expurgarHistorico();
        log.info("Job de expurgo do histórico executado; {} registros removidos", removidos);
    }
}
