package com.fablab.producao.dto;

import com.fablab.producao.entity.Parametro5S;

/** Representação de um parâmetro 5S. */
public record Parametro5SResponse(Long idParametro, String chave, String valor, String descricao) {

    public static Parametro5SResponse from(Parametro5S parametro) {
        return new Parametro5SResponse(
                parametro.getIdParametro(), parametro.getChave(), parametro.getValor(), parametro.getDescricao());
    }
}