package com.fablab.vendas.dto;

import com.fablab.vendas.entity.TipoInteracao;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;

/**
 * Payload de registro de interação com cliente.
 */
public record InteracaoRequest(
        @NotNull Long idCliente,
        LocalDateTime dataInteracao,
        @NotNull TipoInteracao tipo,
        @Size(max = 500) String descricao,
        Long idUsuario) {
}