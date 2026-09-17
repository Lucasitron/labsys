package com.fablab.notification.dto;

import java.time.Instant;

/** Evento {@code nivel.alterado.event} consumido do RH Service. */
public record NivelAlteradoEvent(
        Long idFuncionario,
        String nivelAntigo,
        String nivelNovo,
        Instant data) {
}
