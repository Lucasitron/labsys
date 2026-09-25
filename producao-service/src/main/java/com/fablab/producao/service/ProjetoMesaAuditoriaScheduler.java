package com.fablab.producao.service;

import java.time.LocalDate;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Job periódico que sinaliza projetos de mesa ativos sem evolução há mais de
 * {@code diasParaAuditoriaProjeto} dias, para que o Admin realize a auditoria.
 */
@Component
@ConditionalOnProperty(name = "producao.auditoria.scheduler.enabled", havingValue = "true", matchIfMissing = true)
public class ProjetoMesaAuditoriaScheduler {

    public static final String PARAMETRO_DIAS = "diasParaAuditoriaProjeto";

    private static final Logger log = LoggerFactory.getLogger(ProjetoMesaAuditoriaScheduler.class);

    private final ProjetoMesaService projetoMesaService;
    private final Parametro5SService parametro5SService;

    public ProjetoMesaAuditoriaScheduler(ProjetoMesaService projetoMesaService,
                                         Parametro5SService parametro5SService) {
        this.projetoMesaService = projetoMesaService;
        this.parametro5SService = parametro5SService;
    }

    /** Executa diariamente às 6h. */
    @Scheduled(cron = "${producao.auditoria.scheduler.cron:0 0 6 * * *}")
    public void verificarProjetosSemEvolucao() {
        int dias = parametro5SService.obterInteiro(PARAMETRO_DIAS, 15);
        LocalDate limite = LocalDate.now().minusDays(dias);
        List<Long> pendentes = projetoMesaService.projetosSemEvolucao(limite).stream()
                .map(dto -> dto.idProjetoMesa())
                .toList();
        if (!pendentes.isEmpty()) {
            log.warn("Projetos de mesa sem evolução há mais de {} dias (revisar auditoria): {}", dias, pendentes);
        }
    }
}