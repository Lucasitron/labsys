package com.fablab.producao.dto;

import jakarta.validation.constraints.NotNull;

/** Item avaliado em uma inspeção 5S. */
public record ItemInspecaoRequest(
        @NotNull Long idChecklist,
        @NotNull Boolean conforme,
        String observacao) {
}