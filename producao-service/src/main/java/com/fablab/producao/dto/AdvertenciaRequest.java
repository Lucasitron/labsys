package com.fablab.producao.dto;

import com.fablab.producao.entity.TipoAdvertencia;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

/** Dados de entrada para registrar uma advertência. */
public record AdvertenciaRequest(
        @NotNull Long idFuncionario,
        Long idInspecao,
        LocalDate data,
        @NotBlank @Size(max = 500) String motivo,
        @NotNull TipoAdvertencia tipo) {
}