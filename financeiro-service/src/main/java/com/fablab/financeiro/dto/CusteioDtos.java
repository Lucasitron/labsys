package com.fablab.financeiro.dto;

import com.fablab.financeiro.entity.CustoEncomenda;
import com.fablab.financeiro.entity.FechamentoEncomenda;
import com.fablab.financeiro.entity.StatusFechamento;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;

/** DTOs de fechamento de encomenda e custo calculado (Job Order Costing). */
public final class CusteioDtos {

    private CusteioDtos() {
    }

    public record FechamentoRequest(
            @NotNull(message = "O id da encomenda é obrigatório")
            Integer idEncomenda,
            @NotNull(message = "As horas estimadas são obrigatórias")
            @DecimalMin(value = "0.00", message = "As horas estimadas não podem ser negativas")
            BigDecimal horasEstimadas,
            @NotNull(message = "O valor fechado é obrigatório")
            @DecimalMin(value = "0.00", message = "O valor fechado não pode ser negativo")
            BigDecimal valorFechado,
            LocalDate dataFechamento) {
    }

    public record FechamentoResponse(
            Long id,
            Integer idEncomenda,
            BigDecimal horasEstimadas,
            BigDecimal valorFechado,
            LocalDate dataFechamento,
            StatusFechamento status,
            BigDecimal horasValidadas) {
        public static FechamentoResponse of(FechamentoEncomenda entity) {
            return new FechamentoResponse(entity.getId(), entity.getIdEncomenda(),
                    entity.getHorasEstimadas(), entity.getValorFechado(), entity.getDataFechamento(),
                    entity.getStatus(), entity.getHorasValidadas());
        }
    }

    public record CustoResponse(
            Long id,
            Integer idEncomenda,
            BigDecimal custoMateriais,
            BigDecimal custoMaoObra,
            BigDecimal custoOverhead,
            BigDecimal custoTotal,
            BigDecimal valorVenda,
            BigDecimal margemLucro,
            LocalDate dataCalculo) {
        public static CustoResponse of(CustoEncomenda entity) {
            return new CustoResponse(entity.getId(), entity.getIdEncomenda(),
                    entity.getCustoMateriais(), entity.getCustoMaoObra(), entity.getCustoOverhead(),
                    entity.getCustoTotal(), entity.getValorVenda(), entity.getMargemLucro(),
                    entity.getDataCalculo());
        }
    }
}
