package com.fablab.notification.dto;

/** Evento {@code projeto.mesa.abandonado.event} consumido do Produção Service. */
public record ProjetoMesaAbandonadoEvent(
        Long idProjetoMesa,
        Long idFuncionario,
        String acaoTomada) {
}
