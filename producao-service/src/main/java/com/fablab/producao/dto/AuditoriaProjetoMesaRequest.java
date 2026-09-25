package com.fablab.producao.dto;

import com.fablab.producao.entity.StatusProjetoMesa;
import jakarta.validation.constraints.NotNull;

/** Dados de entrada para auditar um projeto de mesa. */
public record AuditoriaProjetoMesaRequest(
        @NotNull Long idProjetoMesa,
        @NotNull StatusProjetoMesa resultado,
        String acaoTomada) {
}