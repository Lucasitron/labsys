package com.fablab.financeiro.dto;

import com.fablab.financeiro.entity.LancamentoFinanceiro;
import com.fablab.financeiro.entity.StatusLancamento;
import com.fablab.financeiro.entity.TipoLancamento;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/** DTOs de lançamentos financeiros (contas a pagar/receber). */
public final class LancamentoDtos {

    private LancamentoDtos() {
    }

    public record LancamentoRequest(
            @NotNull(message = "A categoria é obrigatória")
            Long idCategoria,
            @NotNull(message = "O tipo é obrigatório (ENTRADA ou SAIDA)")
            TipoLancamento tipo,
            @NotNull(message = "O valor é obrigatório")
            @DecimalMin(value = "0.01", message = "O valor deve ser maior que zero")
            BigDecimal valor,
            @NotNull(message = "A data de vencimento é obrigatória")
            LocalDate dataVencimento,
            @Size(max = 100, message = "A referência externa deve ter no máximo 100 caracteres")
            String idReferenciaExterna,
            @Size(max = 1000, message = "A observação deve ter no máximo 1000 caracteres")
            String observacao) {
    }

    public record PagamentoRequest(
            LocalDate dataPagamento,
            @Size(max = 1000, message = "A observação deve ter no máximo 1000 caracteres")
            String observacao) {
    }

    public record LancamentoResponse(
            Long id,
            Long idCategoria,
            TipoLancamento tipo,
            BigDecimal valor,
            LocalDate dataVencimento,
            LocalDate dataPagamento,
            StatusLancamento status,
            String idReferenciaExterna,
            String observacao) {
        public static LancamentoResponse of(LancamentoFinanceiro entity) {
            return new LancamentoResponse(
                    entity.getId(), entity.getIdCategoria(), entity.getTipo(), entity.getValor(),
                    entity.getDataVencimento(), entity.getDataPagamento(), entity.getStatus(),
                    entity.getIdReferenciaExterna(), entity.getObservacao());
        }
    }

    /** Lista servida de lançamentos com shapes agregados (D-2: counts + resumo). */
    public record LancamentoListaResponse(
            List<LancamentoResponse> lancamentos,
            Map<String, Long> counts,
            ResumoResponse resumo) {
    }

    /** Totais servidos pelo backend; o front nunca soma no client (D-2). */
    public record ResumoResponse(
            BigDecimal entradas,
            BigDecimal saidas,
            BigDecimal pendente,
            BigDecimal saldo) {
    }
}
