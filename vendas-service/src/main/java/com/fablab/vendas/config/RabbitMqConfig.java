package com.fablab.vendas.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuração do RabbitMQ: publicação dos eventos comerciais para o Financeiro
 * e o Notification Service e consumo dos eventos de status do Produção &amp;
 * Projetos e de consumo real do Estoque Service.
 */
@Configuration
public class RabbitMqConfig {

    /** Exchange tópica do domínio de vendas. */
    public static final String VENDAS_EXCHANGE = "fablab.vendas";

    /** Exchange tópica onde o Financeiro Service publica eventos. */
    public static final String FINANCEIRO_EXCHANGE = "fablab.financeiro";

    /** Exchange tópica onde o Notification Service publica eventos. */
    public static final String NOTIFICACAO_EXCHANGE = "fablab.notificacao";

    /** Exchange tópica onde o Produção &amp; Projetos Service publica eventos. */
    public static final String PRODUCAO_EXCHANGE = "fablab.producao";

    /** Exchange tópica onde o Estoque Service publica eventos. */
    public static final String ESTOQUE_EXCHANGE = "fablab.estoque";

    /** Routing keys dos eventos publicados pelo Vendas. */
    public static final String ORCAMENTO_APROVADO_ROUTING_KEY = "orcamento.aprovado.event";
    public static final String ENCOMENDA_ENTREGUE_ROUTING_KEY = "encomenda.entregue.event";
    public static final String MARKETPLACE_VENDA_ROUTING_KEY = "marketplace.venda.event";
    public static final String ENCOMENDA_STATUS_ALTERADO_ROUTING_KEY = "encomenda.status.alterado.event";
    public static final String ENCOMENDA_CRIADA_ROUTING_KEY = "encomenda.criada.event";

    /** Routing key do evento de status de produção do Produção &amp; Projetos. */
    public static final String PRODUCAO_STATUS_ALTERADO_ROUTING_KEY = "producao.status.alterado.event";

    /** Routing key do evento de consumo real de estoque do Estoque Service. */
    public static final String ESTOQUE_CONSUMO_ROUTING_KEY = "estoque.consumo.realizado.event";

    /** Fila de atualizações de status vindas do Produção &amp; Projetos. */
    public static final String KANBAN_PRODUCAO_QUEUE = "vendas.kanban.producao";

    /** Fila informativa de consumo real de estoque associado à encomenda. */
    public static final String ESTOQUE_CONSUMO_QUEUE = "vendas.estoque.consumo";

    @Bean
    public TopicExchange vendasExchange() {
        return new TopicExchange(VENDAS_EXCHANGE, true, false);
    }

    @Bean
    public TopicExchange financeiroExchange() {
        return new TopicExchange(FINANCEIRO_EXCHANGE, true, false);
    }

    @Bean
    public TopicExchange notificacaoExchange() {
        return new TopicExchange(NOTIFICACAO_EXCHANGE, true, false);
    }

    @Bean
    public TopicExchange producaoExchange() {
        return new TopicExchange(PRODUCAO_EXCHANGE, true, false);
    }

    @Bean
    public TopicExchange estoqueExchange() {
        return new TopicExchange(ESTOQUE_EXCHANGE, true, false);
    }

    @Bean
    public Queue kanbanProducaoQueue() {
        return new Queue(KANBAN_PRODUCAO_QUEUE, true);
    }

    @Bean
    public Binding kanbanProducaoBinding(Queue kanbanProducaoQueue, TopicExchange producaoExchange) {
        return BindingBuilder.bind(kanbanProducaoQueue)
                .to(producaoExchange).with(PRODUCAO_STATUS_ALTERADO_ROUTING_KEY);
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
}