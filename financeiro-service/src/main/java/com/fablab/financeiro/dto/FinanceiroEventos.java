package com.fablab.financeiro.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Eventos publicados e consumidos pelo Financeiro via RabbitMQ. */
public final class FinanceiroEventos {

    private FinanceiroEventos() {
    }

    /** Publicado quando um lançamento vence sem liquidação (→ Notification). */
    public record LancamentoVencidoEvent(
            Long idLancamento,
            BigDecimal valor,
            LocalDate dataVencimento,
            String idReferenciaExterna) {
    }

    /** Publicado ao concluir o custeio (→ Vendas &amp; CRM). */
    public record CustoCalculadoEvent(
            Integer idEncomenda,
            BigDecimal custoTotal,
            BigDecimal margemLucro,
            LocalDate dataCalculo) {
    }

    /** Publicado ao registrar solicitação (→ Estoque; {@code idFornecedor} null no MVP). */
    public record CompraSolicitadaEvent(
            Long idCompra,
            Integer idFornecedor) {
    }

    /** Consumido de {@code fablab.vendas}: abre o fechamento em ABERTA. */
    public record EncomendaCriadaEvent(
            Integer idEncomenda,
            BigDecimal valorFinal,
            LocalDate dataCriacao) {
    }

    /** Consumido de {@code fablab.rh}: horas validadas por funcionário/dia. */
    public record HorasValidadasEvent(
            Integer idEncomenda,
            Integer idFuncionario,
            Integer nivelAcesso,
            BigDecimal horas,
            LocalDate dataRegistro) {
    }

    /** Consumido de {@code fablab.producao}: dispara o cálculo de custo. */
    public record ProducaoConcluidaEvent(
            Integer idEncomenda,
            LocalDate dataConclusao) {
    }
}
