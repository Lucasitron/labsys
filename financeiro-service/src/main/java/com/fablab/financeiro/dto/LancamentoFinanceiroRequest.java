package com.fablab.financeiro.dto;

import com.fablab.financeiro.entity.TipoLancamento;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Payload de criação de lançamento financeiro (conta a pagar/receber).
 */
public record LancamentoFinanceiroRequest(
        @NotNull Long idCategoria,
        @NotNull TipoLancamento tipo,
        @NotNull @Positive BigDecimal valor,
        @NotNull LocalDate dataVencimento,
        @Size(max = 100) String idReferenciaExterna,
        @Size(max = 500) String observacao) {
}