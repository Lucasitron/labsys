package com.fablab.vendas.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.List;

/**
 * Payload de criação/atualização de orçamento.
 */
public record OrcamentoRequest(
        @NotNull Long idCliente,
        @NotNull LocalDate validade,
        @Size(max = 500) String observacoes,
        @NotEmpty List<@Valid ItemOrcamentoRequest> itens) {
}