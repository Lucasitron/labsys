package com.fablab.notification.dto;

import java.time.LocalDate;

/** Evento {@code emprestimo.atrasado.event} consumido do Estoque Service. */
public record EmprestimoAtrasadoEvent(
        Long idEmprestimo,
        Long idPessoa,
        Long idItem,
        LocalDate dataDevolucaoPrevista) {
}
