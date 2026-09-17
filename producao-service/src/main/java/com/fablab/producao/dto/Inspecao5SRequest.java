package com.fablab.producao.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.List;

/** Dados de entrada para registrar uma inspeção 5S. */
public record Inspecao5SRequest(
        @NotNull Long idSetor,
        @NotNull Long idInspetor,
        @NotNull LocalDate dataInspecao,
        @NotNull com.fablab.producao.entity.TurnoInspecao turno,
        String observacoes,
        @NotNull @Valid List<ItemInspecaoRequest> itens) {
}