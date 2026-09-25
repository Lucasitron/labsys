package com.fablab.producao.dto;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;

/** Dados de entrada para iniciar o uso de uma máquina. */
public record UsoMaquinaRequest(
        @NotNull Long idFuncionario,
        LocalDateTime dataInicio,
        String observacao) {
}