package com.fablab.producao.dto;

import com.fablab.producao.entity.MaquinaStatus;
import jakarta.validation.constraints.NotNull;

/** Alteração de status de uma máquina. */
public record MaquinaStatusRequest(@NotNull MaquinaStatus status) {
}