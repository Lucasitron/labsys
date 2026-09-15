package com.fablab.vendas.dto;

import com.fablab.vendas.entity.InteracaoCliente;
import com.fablab.vendas.entity.TipoInteracao;
import java.time.LocalDateTime;

/**
 * Resposta de interação registrada com um cliente.
 */
public record InteracaoResponse(
        Long id,
        Long idCliente,
        LocalDateTime dataInteracao,
        TipoInteracao tipo,
        String descricao,
        Long idUsuario) {

    public static InteracaoResponse of(InteracaoCliente interacao) {
        return new InteracaoResponse(
                interacao.getId(),
                interacao.getCliente().getId(),
                interacao.getDataInteracao(),
                interacao.getTipo(),
                interacao.getDescricao(),
                interacao.getIdUsuario());
    }
}