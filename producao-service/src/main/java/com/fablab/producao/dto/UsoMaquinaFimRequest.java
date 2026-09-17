package com.fablab.producao.dto;

import java.time.LocalDateTime;

/** Dados de entrada para encerrar o uso de uma máquina. */
public record UsoMaquinaFimRequest(
        LocalDateTime dataFim,
        String observacao) {
}