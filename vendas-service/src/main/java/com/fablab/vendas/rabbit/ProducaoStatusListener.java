package com.fablab.vendas.rabbit;

import com.fablab.vendas.config.RabbitConfig;
import com.fablab.vendas.entity.Encomenda;
import com.fablab.vendas.entity.HistoricoStatusEncomenda;
import com.fablab.vendas.entity.StatusKanban;
import com.fablab.vendas.rabbit.VendasEventos.ProducaoStatusAlteradoEvent;
import com.fablab.vendas.repository.EncomendaRepository;
import com.fablab.vendas.repository.HistoricoStatusEncomendaRepository;
import java.time.LocalDateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Consumidor idempotente de {@code producao.status.alterado.event}: sincroniza
 * o {@code status_kanban} da encomenda (por {@code idEncomenda}).
 */
@Component
public class ProducaoStatusListener {

    private static final Logger log = LoggerFactory.getLogger(ProducaoStatusListener.class);

    private final EncomendaRepository encomendaRepository;
    private final HistoricoStatusEncomendaRepository historicoRepository;

    public ProducaoStatusListener(EncomendaRepository encomendaRepository,
                                  HistoricoStatusEncomendaRepository historicoRepository) {
        this.encomendaRepository = encomendaRepository;
        this.historicoRepository = historicoRepository;
    }

    @RabbitListener(queues = RabbitConfig.PRODUCAO_STATUS_QUEUE)
    @Transactional
    public void onProducaoStatus(ProducaoStatusAlteradoEvent event) {
        encomendaRepository.findById(event.idEncomenda()).ifPresentOrElse(encomenda -> {
            StatusKanban novo;
            try {
                novo = StatusKanban.valueOf(event.statusNovo());
            } catch (IllegalArgumentException ex) {
                log.warn("Status desconhecido da produção: {}", event.statusNovo());
                return;
            }
            if (encomenda.getStatusKanban() == novo) {
                return;
            }
            HistoricoStatusEncomenda historico = new HistoricoStatusEncomenda();
            historico.setIdEncomenda(encomenda.getId());
            historico.setStatusAnterior(encomenda.getStatusKanban().name());
            historico.setStatusNovo(novo.name());
            historico.setDataAlteracao(LocalDateTime.now());
            historico.setIdUsuario(0L);
            historico.setObservacao(event.observacao() != null ? event.observacao()
                    : "Sincronizado pela produção");
            encomenda.setStatusKanban(novo);
            encomendaRepository.save(encomenda);
            historicoRepository.save(historico);
        }, () -> log.warn("Encomenda {} do evento da produção não encontrada", event.idEncomenda()));
    }

    /** Variante síncrona para testes (sem broker). */
    @Transactional
    public void aplicar(Encomenda encomenda, StatusKanban novo, String observacao, Long idUsuario) {
        if (encomenda.getStatusKanban() == novo) {
            return;
        }
        HistoricoStatusEncomenda historico = new HistoricoStatusEncomenda();
        historico.setIdEncomenda(encomenda.getId());
        historico.setStatusAnterior(encomenda.getStatusKanban().name());
        historico.setStatusNovo(novo.name());
        historico.setDataAlteracao(LocalDateTime.now());
        historico.setIdUsuario(idUsuario);
        historico.setObservacao(observacao);
        encomenda.setStatusKanban(novo);
        historicoRepository.save(historico);
    }
}
