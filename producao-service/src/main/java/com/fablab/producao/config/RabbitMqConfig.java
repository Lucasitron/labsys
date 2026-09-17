package com.fablab.producao.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuração do RabbitMQ: publicação dos eventos de produção (Kanban,
 * produção concluída, advertências e abandono de mesa) e consumo dos eventos do
 * Vendas (encomenda criada), do Estoque (consumo de itens) e do RH (nível
 * alterado).
 */
@Configuration
public class RabbitMqConfig {

    /** Exchange tópica do domínio de produção. */
    public static final String PRODUCAO_EXCHANGE = "fablab.producao";

    /** Exchanges tópicas consumidas e de notificação. */
    public static final String VENDAS_EXCHANGE = "fablab.vendas";
    public static final String ESTOQUE_EXCHANGE = "fablab.estoque";
    public static final String RH_EXCHANGE = "fablab.rh";
    public static final String NOTIFICACAO_EXCHANGE = "fablab.notificacao";

    /** Routing keys dos eventos publicados pelo Produção. */
    public static final String PRODUCAO_STATUS_ALTERADO_ROUTING_KEY = "producao.status.alterado.event";
    public static final String PRODUCAO_CONCLUIDA_ROUTING_KEY = "producao.concluida.event";
    public static final String ADVERTENCIA_REGISTRADA_ROUTING_KEY = "advertencia.registrada.event";
    public static final String PROJETO_MESA_ABANDONADO_ROUTING_KEY = "projeto.mesa.abandonado.event";

    /** Routing keys dos eventos consumidos. */
    public static final String ENCOMENDA_CRIADA_ROUTING_KEY = "encomenda.criada.event";
    public static final String ESTOQUE_CONSUMO_ROUTING_KEY = "estoque.consumo.realizado.event";
    public static final String NIVEL_ALTERADO_ROUTING_KEY = "nivel.alterado.event";

    /** Filas do Produção. */
    public static final String ENCOMENDA_CRIADA_QUEUE = "producao.kanban.encomenda";
    public static final String ESTOQUE_CONSUMO_QUEUE = "producao.estoque.consumo";
    public static final String NIVEL_ALTERADO_QUEUE = "producao.rh.nivel";

    @Bean
    public TopicExchange producaoExchange() {
        return new TopicExchange(PRODUCAO_EXCHANGE, true, false);
    }

    @Bean
    public TopicExchange vendasExchange() {
        return new TopicExchange(VENDAS_EXCHANGE, true, false);
    }

    @Bean
    public TopicExchange estoqueExchange() {
        return new TopicExchange(ESTOQUE_EXCHANGE, true, false);
    }

    @Bean
    public TopicExchange rhExchange() {
        return new TopicExchange(RH_EXCHANGE, true, false);
    }

    @Bean
    public TopicExchange notificacaoExchange() {
        return new TopicExchange(NOTIFICACAO_EXCHANGE, true, false);
    }

    @Bean
    public Queue kanbanEncomendaQueue() {
        return new Queue(ENCOMENDA_CRIADA_QUEUE, true);
    }

    @Bean
    public Binding kanbanEncomendaBinding(Queue kanbanEncomendaQueue, TopicExchange vendasExchange) {
        return BindingBuilder.bind(kanbanEncomendaQueue)
                .to(vendasExchange).with(ENCOMENDA_CRIADA_ROUTING_KEY);
    }

    @Bean
    public Queue estoqueConsumoQueue() {
        return new Queue(ESTOQUE_CONSUMO_QUEUE, true);
    }

    @Bean
    public Binding estoqueConsumoBinding(Queue estoqueConsumoQueue, TopicExchange estoqueExchange) {
        return BindingBuilder.bind(estoqueConsumoQueue)
                .to(estoqueExchange).with(ESTOQUE_CONSUMO_ROUTING_KEY);
    }

    @Bean
    public Queue nivelAlteradoQueue() {
        return new Queue(NIVEL_ALTERADO_QUEUE, true);
    }

    @Bean
    public Binding nivelAlteradoBinding(Queue nivelAlteradoQueue, TopicExchange rhExchange) {
        return BindingBuilder.bind(nivelAlteradoQueue)
                .to(rhExchange).with(NIVEL_ALTERADO_ROUTING_KEY);
    }
}