package com.fablab.rh.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Job mensal do extrato de horas: executa no primeiro dia de cada mês às 06:00
 * e publica o evento {@code extrato.mensal.horas.event} para cada funcionário.
 */
@Component
@ConditionalOnProperty(name = "rh.certificado.extrato.scheduler.enabled", havingValue = "true",
        matchIfMissing = true)
public class ExtratoMensalScheduler {

    private static final Logger log = LoggerFactory.getLogger(ExtratoMensalScheduler.class);

    private final ExtratoService extratoService;

    public ExtratoMensalScheduler(ExtratoService extratoService) {
        this.extratoService = extratoService;
    }

    /** Primeiro dia de cada mês às 06:00. */
    @Scheduled(cron = "${rh.certificado.extrato.scheduler.cron:0 0 6 1 * *}")
    public void gerarExtratoMensal() {
        extratoService.gerarExtratoMensal();
        log.info("Extrato mensal de horas gerado e publicado");
    }
}