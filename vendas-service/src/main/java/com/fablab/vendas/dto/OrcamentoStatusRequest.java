package com.fablab.vendas.dto;

import com.fablab.vendas.entity.StatusOrcamento;
import jakarta.validation.constraints.NotNull;

/**
 * Payload de mudança de status de um orçamento (ex.: aprovação).
 */
public record OrcamentoStatusRequest(
        @NotNull StatusOrcamento status) {
}