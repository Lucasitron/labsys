package com.fablab.producao.dto;

/** Evento {@code advertencia.registrada.event} publicado para o Notification. */
public record AdvertenciaRegistradaEvent(
        Long idFuncionario,
        Integer contador,
        String motivo) {
}