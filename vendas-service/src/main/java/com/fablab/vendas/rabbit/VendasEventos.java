package com.fablab.vendas.rabbit;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/** Payloads dos eventos publicados/consumidos pelo Vendas. */
public final class VendasEventos {

    private VendasEventos() {
    }

    public record EncomendaCriadaEvent(Long idEncomenda, Long idCliente, BigDecimal valorFinal,
                                       LocalDate dataCriacao) {
    }

    public record EncomendaStatusAlteradoEvent(Long idEncomenda, String statusAnterior, String statusNovo,
                                               LocalDateTime dataAlteracao) {
    }

    public record OrcamentoAprovadoEvent(Long idOrcamento, Long idCliente, BigDecimal valorTotal) {
    }

    public record ProducaoStatusAlteradoEvent(Long idEncomenda, String statusNovo, String observacao) {
    }
}
