package com.fablab.producao.dto;

/**
 * Evento {@code projeto.mesa.abandonado.event} publicado quando uma auditoria
 * identifica abandono de uma mesa, notificando o RH e o Notification.
 */
public record ProjetoMesaAbandonadoEvent(
        Long idProjetoMesa,
        Long idFuncionario,
        String acaoTomada) {
}