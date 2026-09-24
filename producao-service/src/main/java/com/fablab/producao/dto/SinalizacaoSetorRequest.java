package com.fablab.producao.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Sinalização de um setor 5S. */
public record SinalizacaoSetorRequest(@NotBlank @Size(max = 255) String texto) {
}