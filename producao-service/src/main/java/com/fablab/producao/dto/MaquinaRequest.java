package com.fablab.producao.dto;

import com.fablab.producao.entity.MaquinaStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Dados de entrada para criação/atualização de máquina. */
public record MaquinaRequest(
        @NotBlank @Size(max = 150) String nome,
        @Size(max = 1000) String descricao,
        @Size(max = 150) String localizacao,
        MaquinaStatus status) {
}