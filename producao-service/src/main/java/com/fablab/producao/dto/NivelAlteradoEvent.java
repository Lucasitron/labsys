package com.fablab.producao.dto;

import java.time.Instant;

/** Evento {@code nivel.alterado.event} publicado pelo Pessoas &amp; RH. */
public record NivelAlteradoEvent(
        Long idFuncionario,
        String nivelAntigo,
        String nivelNovo,
        Instant data) {
}