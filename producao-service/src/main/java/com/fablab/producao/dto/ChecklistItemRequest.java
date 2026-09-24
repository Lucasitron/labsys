package com.fablab.producao.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Item de checklist de um setor 5S. */
public record ChecklistItemRequest(
        @NotBlank @Size(max = 255) String item,
        Boolean ativo) {
}