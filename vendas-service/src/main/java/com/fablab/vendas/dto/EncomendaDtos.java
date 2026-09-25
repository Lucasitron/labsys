package com.fablab.vendas.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/** DTOs de encomendas e Kanban. */
public final class EncomendaDtos {

    private EncomendaDtos() {
    }

    public record EncomendaRequest(
            Long idOrcamento,
            Long clienteId,
            BigDecimal valorFinal,
            LocalDate dataPrevisaoEntrega,
            String observacoes) {
    }

    public record EncomendaResponse(
            Long id,
            Long idOrcamento,
            Long idCliente,
            String clienteNome,
            LocalDate dataCriacao,
            LocalDate dataPrevisaoEntrega,
            String statusKanban,
            BigDecimal valorFinal,
            String origem,
            int itensCount,
            Long versao,
            Long criadoPor) {
    }

    public record EncomendaDetalheResponse(
            Long id,
            Long idOrcamento,
            Long idCliente,
            String clienteNome,
            LocalDate dataCriacao,
            LocalDate dataPrevisaoEntrega,
            String statusKanban,
            BigDecimal valorFinal,
            String observacoes,
            String origem,
            Long versao,
            Long criadoPor,
            Long encomendaOrigemId,
            List<OrcamentoDtos.ItemOrcamentoResponse> itens,
            List<HistoricoResponse> historico) {
    }

    public record EncomendaListaResponse(
            List<EncomendaResponse> encomendas,
            Map<String, Long> counts) {
    }

    public record KanbanRequest(
            @NotBlank(message = "O status de destino é obrigatório")
            String statusKanban,

            @NotNull(message = "A versão do cartão é obrigatória (lock otimista)")
            Long versao,

            String observacao) {
    }

    public record NovaOrdemRequest(
            LocalDate dataPrevisaoEntrega,

            @NotNull(message = "O valor da nova ordem é obrigatório")
            @DecimalMin(value = "0.0", message = "O valor não pode ser negativo")
            BigDecimal valorFinal,

            String observacoes) {
    }

    public record HistoricoResponse(
            Long id,
            String statusAnterior,
            String statusNovo,
            LocalDateTime dataAlteracao,
            Long idUsuario,
            String observacao) {
    }

    public record StatusResponse(
            Long id,
            String statusKanban,
            LocalDate dataPrevisaoEntrega,
            BigDecimal valorFinal) {
    }
}
