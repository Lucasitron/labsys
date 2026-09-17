package com.fablab.notification.dto;

/** Evento {@code advertencia.registrada.event} consumido do Produção Service. */
public record AdvertenciaRegistradaEvent(
        Long idFuncionario,
        Integer contador,
        String motivo) {
}
