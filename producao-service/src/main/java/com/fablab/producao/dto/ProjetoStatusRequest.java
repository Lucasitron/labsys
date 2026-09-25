package com.fablab.producao.dto;

import com.fablab.producao.entity.ProjetoStatus;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

/** Alteração de status de um projeto. */
public record ProjetoStatusRequest(
        @NotNull ProjetoStatus status,
        LocalDate dataFimReal) {
}