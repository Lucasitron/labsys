package com.fablab.producao.dto;

import com.fablab.producao.entity.SetorSinalizacao;

/** Representação de uma sinalização de setor. */
public record SinalizacaoSetorResponse(Long idSinalizacao, String texto) {

    public static SinalizacaoSetorResponse from(SetorSinalizacao sinalizacao) {
        return new SinalizacaoSetorResponse(sinalizacao.getIdSinalizacao(), sinalizacao.getTexto());
    }
}