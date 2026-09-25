package com.fablab.producao.service;

import com.fablab.producao.config.RabbitMqConfig;
import com.fablab.producao.dto.AdvertenciaLimiteEvent;
import com.fablab.producao.dto.AdvertenciaRegistradaEvent;
import com.fablab.producao.dto.KanbanStatusAlteradoEvent;
import com.fablab.producao.dto.ProducaoConcluidaEvent;
import com.fablab.producao.dto.ProducaoStatusEvent;
import com.fablab.producao.dto.ProjetoMesaAbandonadoEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

/** Publica os eventos de domínio do Produção &amp; Projetos Service. */
@Service
public class ProducaoEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(ProducaoEventPublisher.class);

    private final RabbitTemplate rabbitTemplate;

    public ProducaoEventPublisher(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    /** Notifica o Vendas sobre a mudança de coluna do Kanban. */
    public void publicarStatusAlterado(ProducaoStatusEvent event) {
        log.info("Publicando producao.status.alterado.event para a encomenda {}", event.idEncomenda());
        rabbitTemplate.convertAndSend(RabbitMqConfig.PRODUCAO_EXCHANGE,
                RabbitMqConfig.PRODUCAO_STATUS_ALTERADO_ROUTING_KEY, event);
    }

    /** Notifica Estoque e Financeiro sobre a conclusão da produção. */
    public void publicarProducaoConcluida(ProducaoConcluidaEvent event) {
        log.info("Publicando producao.concluida.event para a encomenda {}", event.idEncomenda());
        rabbitTemplate.convertAndSend(RabbitMqConfig.PRODUCAO_EXCHANGE,
                RabbitMqConfig.PRODUCAO_CONCLUIDA_ROUTING_KEY, event);
    }

    /** Notifica Vendas e Notification sobre a mudança de coluna do Kanban. */
    public void publicarKanbanStatusAlterado(KanbanStatusAlteradoEvent event) {
        log.info("Publicando kanban.status.alterado.event para a encomenda {}", event.idEncomenda());
        rabbitTemplate.convertAndSend(RabbitMqConfig.NOTIFICACAO_EXCHANGE,
                RabbitMqConfig.KANBAN_STATUS_ALTERADO_ROUTING_KEY, event);
    }

    /** Notifica o Notification sobre uma advertência registrada. */
    public void publicarAdvertencia(AdvertenciaRegistradaEvent event) {
        log.info("Publicando advertencia.registrada.event para o funcionário {}", event.idFuncionario());
        rabbitTemplate.convertAndSend(RabbitMqConfig.NOTIFICACAO_EXCHANGE,
                RabbitMqConfig.ADVERTENCIA_REGISTRADA_ROUTING_KEY, event);
    }

    /** Emite alerta crítico quando o membro atinge o limite de advertências. */
    public void publicarAdvertenciaLimite(AdvertenciaLimiteEvent event) {
        log.warn("Publicando advertencia.limite.atingido.event para o funcionário {}", event.idFuncionario());
        rabbitTemplate.convertAndSend(RabbitMqConfig.NOTIFICACAO_EXCHANGE,
                RabbitMqConfig.ADVERTENCIA_LIMITE_ROUTING_KEY, event);
    }

    /** Notifica RH e Notification sobre abandono de mesa. */
    public void publicarProjetoMesaAbandonado(ProjetoMesaAbandonadoEvent event) {
        log.info("Publicando projeto.mesa.abandonado.event para o projeto de mesa {}", event.idProjetoMesa());
        rabbitTemplate.convertAndSend(RabbitMqConfig.NOTIFICACAO_EXCHANGE,
                RabbitMqConfig.PROJETO_MESA_ABANDONADO_ROUTING_KEY, event);
    }
}