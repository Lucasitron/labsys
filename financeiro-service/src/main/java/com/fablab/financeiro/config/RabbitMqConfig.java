package com.fablab.financeiro.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuração do RabbitMQ: publicação dos eventos financeiros (Notification e
 * Notification de vencimentos, custo calculado para o Vendas e solicitação de
 * compra para o Estoque) e consumo dos eventos do Vendas, do RH e do Produção.
 */
@Configuration
public class RabbitMqConfig {

    /** Exchange tópica do domínio financeiro. */
    public static final String FINANCEIRO_EXCHANGE = "fablab.financeiro";

    /** Exchanges tópicas dos domínios produtores consumidos. */
    public static final String VENDAS_EXCHANGE = "fablab.vendas";
    public static final String RH_EXCHANGE = "fablab.rh";
    public static final String PRODUCAO_EXCHANGE = "fablab.producao";

    /** Routing keys dos eventos publicados pelo Financeiro. */
    public static final String LANCAMENTO_VENCIDO_ROUTING_KEY = "lancamento.vencido.event";
    public static final String CUSTO_CALCULADO_ROUTING_KEY = "custo.calculado.event";
    public static final String COMPRA_SOLICITADA_ROUTING_KEY = "compra.solicitada.event";

    /** Routing keys dos eventos consumidos. */
    public static final String ENCOMENDA_CRIADA_ROUTING_KEY = "encomenda.criada.event";
    public static final String HORAS_VALIDADAS_ROUTING_KEY = "horas.validadas.event";
    public static final String PRODUCAO_CONCLUIDA_ROUTING_KEY = "producao.concluida.event";

    /** Filas do Financeiro. */
    public static final String ENCOMENDA_CRIADA_QUEUE = "financeiro.fechamento.encomenda";
    public static final String HORAS_VALIDADAS_QUEUE = "financeiro.horas.encomenda";
    public static final String PRODUCAO_CONCLUIDA_QUEUE = "financeiro.custo.calculo";

    @Bean
    public TopicExchange financeiroExchange() {
        return new TopicExchange(FINANCEIRO_EXCHANGE, true, false);
    }

    @Bean
    public TopicExchange vendasExchange() {
        return new TopicExchange(VENDAS_EXCHANGE, true, false);
    }

    @Bean
    public TopicExchange rhExchange() {
        return new TopicExchange(RH_EXCHANGE, true, false);
    }

    @Bean
    public TopicExchange producaoExchange() {
        return new TopicExchange(PRODUCAO_EXCHANGE, true, false);
    }

    @Bean
    public Queue fechamentoEncomendaQueue() {
        return new Queue(ENCOMENDA_CRIADA_QUEUE, true);
    }

    @Bean
    public Binding fechamentoEncomendaBinding(Queue fechamentoEncomendaQueue, TopicExchange vendasExchange) {
        return BindingBuilder.bind(fechamentoEncomendaQueue)
                .to(vendasExchange).with(ENCOMENDA_CRIADA_ROUTING_KEY);
    }

    @Bean
    public Queue horasValidadasQueue() {
        return new Queue(HORAS_VALIDADAS_QUEUE, true);
    }

    @Bean
    public Binding horasValidadasBinding(Queue horasValidadasQueue, TopicExchange rhExchange) {
        return BindingBuilder.bind(horasValidadasQueue)
                .to(rhExchange).with(HORAS_VALIDADAS_ROUTING_KEY);
    }

    @Bean
    public Queue producaoConcluidaQueue() {
        return new Queue(PRODUCAO_CONCLUIDA_QUEUE, true);
    }

    @Bean
    public Binding producaoConcluidaBinding(Queue producaoConcluidaQueue, TopicExchange producaoExchange) {
        return BindingBuilder.bind(producaoConcluidaQueue)
                .to(producaoExchange).with(PRODUCAO_CONCLUIDA_ROUTING_KEY);
    }
}