package com.fablab.producao.dto;

import com.fablab.producao.entity.Maquina;
import com.fablab.producao.entity.MaquinaStatus;

/** Representação de uma máquina. */
public record MaquinaResponse(
        Long idMaquina,
        String nome,
        String descricao,
        String localizacao,
        MaquinaStatus status) {

    public static MaquinaResponse from(Maquina maquina) {
        return new MaquinaResponse(
                maquina.getIdMaquina(),
                maquina.getNome(),
                maquina.getDescricao(),
                maquina.getLocalizacao(),
                maquina.getStatus());
    }
}