package com.fablab.notification.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuração do RabbitMQ: o Notification Service apenas consome eventos,
 * criando uma fila por tipo de evento e ligando-a à exchange de origem.
 */
@Configuration
public class RabbitMqConfig {

    /** Exchanges tópicas consumidas. */
    public static final String NOTIFICACAO_EXCHANGE = "fablab.notificacao";
    public static final String VENDAS_EXCHANGE = "fablab.vendas";
    public static final String ESTOQUE_EXCHANGE = "fablab.estoque";
    public static final String FINANCEIRO_EXCHANGE = "fablab.financeiro";
    public static final String RH_EXCHANGE = "fablab.rh";

    /** Routing keys consumidas. */
    public static final String ESTOQUE_BAIXO_ROUTING_KEY = "estoque.baixo.event";
    public static final String EMPRESTIMO_ATRASADO_ROUTING_KEY = "emprestimo.atrasado.event";
    public static final String ENCOMENDA_CRIADA_ROUTING_KEY = "encomenda.criada.event";
    public static final String ENCOMENDA_STATUS_ALTERADO_ROUTING_KEY = "encomenda.status.alterado.event";
    public static final String ORCAMENTO_APROVADO_ROUTING_KEY = "orcamento.aprovado.event";
    public static final String LANCAMENTO_VENCIDO_ROUTING_KEY = "lancamento.vencido.event";
    public static final String ADVERTENCIA_REGISTRADA_ROUTING_KEY = "advertencia.registrada.event";
    public static final String PROJETO_MESA_ABANDONADO_ROUTING_KEY = "projeto.mesa.abandonado.event";
    public static final String KANBAN_STATUS_ALTERADO_ROUTING_KEY = "kanban.status.alterado.event";
    public static final String NIVEL_ALTERADO_ROUTING_KEY = "nivel.alterado.event";
    public static final String HORAS_VALIDADAS_ROUTING_KEY = "horas.validadas.event";
    public static final String COMPRA_SOLICITADA_ROUTING_KEY = "compra.solicitada.event";
    public static final String CERTIFICADO_SOLICITADO_ROUTING_KEY = "certificado.solicitado.event";
    public static final String CERTIFICADO_APROVADO_ROUTING_KEY = "certificado.aprovado.event";
    public static final String CERTIFICADO_REJEITADO_ROUTING_KEY = "certificado.rejeitado.event";
    public static final String EXTRATO_MENSAL_HORAS_ROUTING_KEY = "extrato.mensal.horas.event";

    /** Filas do Notification Service. */
    public static final String ESTOQUE_BAIXO_QUEUE = "notificacao.estoque.baixo";
    public static final String EMPRESTIMO_ATRASADO_QUEUE = "notificacao.emprestimo.atrasado";
    public static final String ENCOMENDA_CRIADA_QUEUE = "notificacao.encomenda.criada";
    public static final String ENCOMENDA_STATUS_QUEUE = "notificacao.encomenda.status";
    public static final String ORCAMENTO_APROVADO_QUEUE = "notificacao.orcamento.aprovado";
    public static final String LANCAMENTO_VENCIDO_QUEUE = "notificacao.lancamento.vencido";
    public static final String ADVERTENCIA_QUEUE = "notificacao.advertencia";
    public static final String PROJETO_MESA_QUEUE = "notificacao.projeto.mesa";
    public static final String KANBAN_STATUS_QUEUE = "notificacao.kanban.status";
    public static final String NIVEL_ALTERADO_QUEUE = "notificacao.nivel.alterado";
    public static final String HORAS_VALIDADAS_QUEUE = "notificacao.horas.validadas";
    public static final String COMPRA_SOLICITADA_QUEUE = "notificacao.compra.solicitada";
    public static final String CERTIFICADO_SOLICITADO_QUEUE = "notificacao.certificado.solicitado";
    public static final String CERTIFICADO_APROVADO_QUEUE = "notificacao.certificado.aprovado";
    public static final String CERTIFICADO_REJEITADO_QUEUE = "notificacao.certificado.rejeitado";
    public static final String EXTRATO_MENSAL_HORAS_QUEUE = "notificacao.extrato.mensal.horas";

    @Bean
    public TopicExchange notificacaoExchange() {
        return new TopicExchange(NOTIFICACAO_EXCHANGE, true, false);
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
    public TopicExchange financeiroExchange() {
        return new TopicExchange(FINANCEIRO_EXCHANGE, true, false);
    }

    @Bean
    public TopicExchange rhExchange() {
        return new TopicExchange(RH_EXCHANGE, true, false);
    }

    @Bean
    public Binding estoqueBaixoBinding(Queue estoqueBaixoQueue, TopicExchange estoqueExchange) {
        return bind(estoqueBaixoQueue, estoqueExchange, ESTOQUE_BAIXO_ROUTING_KEY);
    }

    @Bean
    public Queue estoqueBaixoQueue() {
        return new Queue(ESTOQUE_BAIXO_QUEUE, true);
    }

    @Bean
    public Binding emprestimoAtrasadoBinding(Queue emprestimoAtrasadoQueue, TopicExchange estoqueExchange) {
        return bind(emprestimoAtrasadoQueue, estoqueExchange, EMPRESTIMO_ATRASADO_ROUTING_KEY);
    }

    @Bean
    public Queue emprestimoAtrasadoQueue() {
        return new Queue(EMPRESTIMO_ATRASADO_QUEUE, true);
    }

    @Bean
    public Binding encomendaCriadaBinding(Queue encomendaCriadaQueue, TopicExchange vendasExchange) {
        return bind(encomendaCriadaQueue, vendasExchange, ENCOMENDA_CRIADA_ROUTING_KEY);
    }

    @Bean
    public Queue encomendaCriadaQueue() {
        return new Queue(ENCOMENDA_CRIADA_QUEUE, true);
    }

    @Bean
    public Binding encomendaStatusBinding(Queue encomendaStatusQueue, TopicExchange vendasExchange) {
        return bind(encomendaStatusQueue, vendasExchange, ENCOMENDA_STATUS_ALTERADO_ROUTING_KEY);
    }

    @Bean
    public Queue encomendaStatusQueue() {
        return new Queue(ENCOMENDA_STATUS_QUEUE, true);
    }

    @Bean
    public Binding orcamentoAprovadoBinding(Queue orcamentoAprovadoQueue, TopicExchange vendasExchange) {
        return bind(orcamentoAprovadoQueue, vendasExchange, ORCAMENTO_APROVADO_ROUTING_KEY);
    }

    @Bean
    public Queue orcamentoAprovadoQueue() {
        return new Queue(ORCAMENTO_APROVADO_QUEUE, true);
    }

    @Bean
    public Binding lancamentoVencidoBinding(Queue lancamentoVencidoQueue, TopicExchange financeiroExchange) {
        return bind(lancamentoVencidoQueue, financeiroExchange, LANCAMENTO_VENCIDO_ROUTING_KEY);
    }

    @Bean
    public Queue lancamentoVencidoQueue() {
        return new Queue(LANCAMENTO_VENCIDO_QUEUE, true);
    }

    @Bean
    public Binding compraSolicitadaBinding(Queue compraSolicitadaQueue, TopicExchange financeiroExchange) {
        return bind(compraSolicitadaQueue, financeiroExchange, COMPRA_SOLICITADA_ROUTING_KEY);
    }

    @Bean
    public Queue compraSolicitadaQueue() {
        return new Queue(COMPRA_SOLICITADA_QUEUE, true);
    }

    @Bean
    public Binding advertenciaBinding(Queue advertenciaQueue, TopicExchange notificacaoExchange) {
        return bind(advertenciaQueue, notificacaoExchange, ADVERTENCIA_REGISTRADA_ROUTING_KEY);
    }

    @Bean
    public Queue advertenciaQueue() {
        return new Queue(ADVERTENCIA_QUEUE, true);
    }

    @Bean
    public Binding projetoMesaBinding(Queue projetoMesaQueue, TopicExchange notificacaoExchange) {
        return bind(projetoMesaQueue, notificacaoExchange, PROJETO_MESA_ABANDONADO_ROUTING_KEY);
    }

    @Bean
    public Queue projetoMesaQueue() {
        return new Queue(PROJETO_MESA_QUEUE, true);
    }

    @Bean
    public Binding kanbanStatusBinding(Queue kanbanStatusQueue, TopicExchange notificacaoExchange) {
        return bind(kanbanStatusQueue, notificacaoExchange, KANBAN_STATUS_ALTERADO_ROUTING_KEY);
    }

    @Bean
    public Queue kanbanStatusQueue() {
        return new Queue(KANBAN_STATUS_QUEUE, true);
    }

    @Bean
    public Binding nivelAlteradoBinding(Queue nivelAlteradoQueue, TopicExchange rhExchange) {
        return bind(nivelAlteradoQueue, rhExchange, NIVEL_ALTERADO_ROUTING_KEY);
    }

    @Bean
    public Queue nivelAlteradoQueue() {
        return new Queue(NIVEL_ALTERADO_QUEUE, true);
    }

    @Bean
    public Binding horasValidadasBinding(Queue horasValidadasQueue, TopicExchange rhExchange) {
        return bind(horasValidadasQueue, rhExchange, HORAS_VALIDADAS_ROUTING_KEY);
    }

    @Bean
    public Queue horasValidadasQueue() {
        return new Queue(HORAS_VALIDADAS_QUEUE, true);
    }

    @Bean
    public Binding certificadoSolicitadoBinding(Queue certificadoSolicitadoQueue, TopicExchange rhExchange) {
        return bind(certificadoSolicitadoQueue, rhExchange, CERTIFICADO_SOLICITADO_ROUTING_KEY);
    }

    @Bean
    public Queue certificadoSolicitadoQueue() {
        return new Queue(CERTIFICADO_SOLICITADO_QUEUE, true);
    }

    @Bean
    public Binding certificadoAprovadoBinding(Queue certificadoAprovadoQueue, TopicExchange rhExchange) {
        return bind(certificadoAprovadoQueue, rhExchange, CERTIFICADO_APROVADO_ROUTING_KEY);
    }

    @Bean
    public Queue certificadoAprovadoQueue() {
        return new Queue(CERTIFICADO_APROVADO_QUEUE, true);
    }

    @Bean
    public Binding certificadoRejeitadoBinding(Queue certificadoRejeitadoQueue, TopicExchange rhExchange) {
        return bind(certificadoRejeitadoQueue, rhExchange, CERTIFICADO_REJEITADO_ROUTING_KEY);
    }

    @Bean
    public Queue certificadoRejeitadoQueue() {
        return new Queue(CERTIFICADO_REJEITADO_QUEUE, true);
    }

    @Bean
    public Binding extratoMensalHorasBinding(Queue extratoMensalHorasQueue, TopicExchange rhExchange) {
        return bind(extratoMensalHorasQueue, rhExchange, EXTRATO_MENSAL_HORAS_ROUTING_KEY);
    }

    @Bean
    public Queue extratoMensalHorasQueue() {
        return new Queue(EXTRATO_MENSAL_HORAS_QUEUE, true);
    }

    private Binding bind(Queue queue, TopicExchange exchange, String routingKey) {
        return BindingBuilder.bind(queue).to(exchange).with(routingKey);
    }
}
