package com.fablab.producao.consumer;

import com.fablab.producao.config.RabbitMqConfig;
import com.fablab.producao.dto.EncomendaCriadaEvent;
import com.fablab.producao.entity.EncomendaKanban;
import com.fablab.producao.entity.KanbanStatus;
import com.fablab.producao.repository.EncomendaKanbanRepository;
import java.time.LocalDateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Consome {@code encomenda.criada.event} do Vendas e insere automaticamente a
 * encomenda na coluna inicial do Kanban de produção.
 */
@Component
public class EncomendaCriadaListener {

    private static final Logger log = LoggerFactory.getLogger(EncomendaCriadaListener.class);

    private final EncomendaKanbanRepository kanbanRepository;

    public EncomendaCriadaListener(EncomendaKanbanRepository kanbanRepository) {
        this.kanbanRepository = kanbanRepository;
    }

    @RabbitListener(queues = RabbitMqConfig.ENCOMENDA_CRIADA_QUEUE)
    @Transactional
    public void onEncomendaCriada(EncomendaCriadaEvent event) {
        if (event.idEncomenda() == null) {
            log.warn("Evento encomenda.criada sem idEncomenda; ignorando");
            return;
        }
        if (kanbanRepository.existsByIdEncomenda(event.idEncomenda())) {
            log.debug("Encomenda {} já possui cartão no Kanban", event.idEncomenda());
            return;
        }
        EncomendaKanban kanban = new EncomendaKanban();
        kanban.setIdEncomenda(event.idEncomenda());
        kanban.setStatus(KanbanStatus.FILA);
        kanban.setDataEntradaStatus(LocalDateTime.now());
        kanban.setOrdem(0);
        kanbanRepository.save(kanban);
        log.info("Encomenda {} inserida no Kanban de produção", event.idEncomenda());
    }
}