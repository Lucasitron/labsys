package com.fablab.producao.dto;

/**
 * Evento crítico {@code advertencia.limite.atingido.event}, emitido quando um
 * membro atinge 3 advertências para que o Admin avalie a suspensão.
 */
public record AdvertenciaLimiteEvent(
        Long idFuncionario,
        Integer contador,
        String motivo) {
}