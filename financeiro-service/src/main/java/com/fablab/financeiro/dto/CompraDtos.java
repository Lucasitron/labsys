package com.fablab.financeiro.dto;

import com.fablab.financeiro.entity.SolicitacaoCompra;
import com.fablab.financeiro.entity.StatusCompra;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;

/** DTOs de solicitações de compra (fluxo informativo). */
public final class CompraDtos {

    private CompraDtos() {
    }

    public record CompraRequest(
            @NotNull(message = "O id do item de estoque é obrigatório")
            Integer idItemEstoque,
            @NotNull(message = "A quantidade é obrigatória")
            @DecimalMin(value = "0.01", message = "A quantidade deve ser maior que zero")
            BigDecimal quantidade,
            @DecimalMin(value = "0.00", message = "O valor estimado não pode ser negativo")
            BigDecimal valorEstimado) {
    }

    public record CompraResponse(
            Long id,
            Integer idItemEstoque,
            BigDecimal quantidade,
            BigDecimal valorEstimado,
            StatusCompra status,
            LocalDate dataSolicitacao) {
        public static CompraResponse of(SolicitacaoCompra entity) {
            return new CompraResponse(entity.getId(), entity.getIdItemEstoque(),
                    entity.getQuantidade(), entity.getValorEstimado(), entity.getStatus(),
                    entity.getDataSolicitacao());
        }
    }
}
