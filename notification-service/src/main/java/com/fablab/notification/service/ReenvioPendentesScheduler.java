package com.fablab.notification.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Job opcional de reenvio: reprocessa periodicamente as notificações
 * {@code PENDENTE}. Desabilitado por padrão e habilitado via
 * {@code notification.reenvio.scheduler.enabled=true}.
 */
@Component
@ConditionalOnProperty(name = "notification.reenvio.scheduler.enabled", havingValue = "true")
public class ReenvioPendentesScheduler {

    private static final Logger log = LoggerFactory.getLogger(ReenvioPendentesScheduler.class);

    private final NotificacaoService notificacaoService;

    public ReenvioPendentesScheduler(NotificacaoService notificacaoService) {
        this.notificacaoService = notificacaoService;
    }

    /** Executa a cada 30 minutos por padrão. */
    @Scheduled(cron = "${notification.reenvio.scheduler.cron:0 */30 * * * *}")
    public void reenviar() {
        int processadas = notificacaoService.reenviarPendentes();
        log.info("Job de reenvio executado; {} notificações pendentes processadas", processadas);
    }
}
