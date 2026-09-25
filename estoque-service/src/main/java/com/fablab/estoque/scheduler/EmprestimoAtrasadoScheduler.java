package com.fablab.estoque.scheduler;

import com.fablab.estoque.service.EmprestimoService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Job diário que marca como ATRASADO os empréstimos com devolução vencida e
 * publica {@code emprestimo.atrasado.event} para o Notification Service.
 *
 * <p>Cron padrão {@code 0 0 3 * * *} (03:00). Configurável e ativável por
 * properties:</p>
 * <ul>
 *   <li>{@code estoque.alerta.emprestimo.scheduler.enabled}</li>
 *   <li>{@code estoque.alerta.emprestimo.scheduler.cron}</li>
 * </ul>
 */
@Component
@ConditionalOnProperty(
        name = "estoque.alerta.emprestimo.scheduler.enabled",
        havingValue = "true",
        matchIfMissing = true)
public class EmprestimoAtrasadoScheduler {

    private static final Logger log = LoggerFactory.getLogger(EmprestimoAtrasadoScheduler.class);

    private final EmprestimoService emprestimoService;

    public EmprestimoAtrasadoScheduler(EmprestimoService emprestimoService) {
        this.emprestimoService = emprestimoService;
    }

    @Scheduled(cron = "${estoque.alerta.emprestimo.scheduler.cron:0 0 3 * * *}")
    public void marcarAtrasados() {
        int marcados = emprestimoService.verificarAtrasados();
        log.info("Job de empréstimos atrasados concluído: {} marcado(s) como atrasado(s)", marcados);
    }
}