package com.fablab.producao.dto;

import com.fablab.producao.entity.AdvertenciaMembro;
import com.fablab.producao.entity.TipoAdvertencia;
import java.time.LocalDate;

/** Representação de uma advertência. */
public record AdvertenciaResponse(
        Long idAdvertencia,
        Long idFuncionario,
        Long idInspecao,
        LocalDate data,
        String motivo,
        TipoAdvertencia tipo,
        Integer contador,
        Long idAdminRegistrou) {

    public static AdvertenciaResponse from(AdvertenciaMembro advertencia) {
        return new AdvertenciaResponse(
                advertencia.getIdAdvertencia(),
                advertencia.getIdFuncionario(),
                advertencia.getInspecao() == null ? null : advertencia.getInspecao().getIdInspecao(),
                advertencia.getData(),
                advertencia.getMotivo(),
                advertencia.getTipo(),
                advertencia.getContador(),
                advertencia.getIdAdminRegistrou());
    }
}