package com.fablab.vendas.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/** DTOs de orçamentos (com extensão de item D-5). */
public final class OrcamentoDtos {

    private OrcamentoDtos() {
    }

    public record ItemOrcamentoDto(
            @NotBlank(message = "A descrição do item é obrigatória")
            String descricao,

            @NotNull(message = "A quantidade do item é obrigatória")
            @DecimalMin(value = "0.01", message = "A quantidade deve ser maior que zero")
            BigDecimal quantidade,

            @NotNull(message = "O valor unitário do item é obrigatório")
            @DecimalMin(value = "0.0", message = "O valor unitário não pode ser negativo")
            BigDecimal valorUnitario,

            String materialTipo,
            BigDecimal materialQuantidade,
            String materialUnidade,
            BigDecimal horas,
            Boolean compra) {
    }

    public record ItemOrcamentoResponse(
            Long id,
            String descricao,
            BigDecimal quantidade,
            BigDecimal valorUnitario,
            BigDecimal subtotal,
            String materialTipo,
            BigDecimal materialQuantidade,
            String materialUnidade,
            BigDecimal horas,
            boolean compra) {
    }

    public record OrcamentoRequest(
            @NotNull(message = "O cliente é obrigatório")
            Long clienteId,

            LocalDate validade,
            String observacoes,

            @NotEmpty(message = "O orçamento precisa de ao menos um item")
            @Valid
            List<ItemOrcamentoDto> itens) {
    }

    public record OrcamentoAtualizacaoRequest(
            LocalDate validade,
            String observacoes,
            String status,
            @Valid
            List<ItemOrcamentoDto> itens) {
    }

    public record OrcamentoResponse(
            Long id,
            Long clienteId,
            String clienteNome,
            LocalDate dataCriacao,
            LocalDate validade,
            BigDecimal valorTotal,
            String status,
            int qtdItens,
            Long criadoPor) {
    }

    public record OrcamentoDetalheResponse(
            Long id,
            Long clienteId,
            String clienteNome,
            LocalDate dataCriacao,
            LocalDate validade,
            BigDecimal valorTotal,
            String status,
            String observacoes,
            Long criadoPor,
            List<ItemOrcamentoResponse> itens) {
    }

    public record OrcamentoListaResponse(
            List<OrcamentoResponse> orcamentos,
            Map<String, Long> counts) {
    }
}
