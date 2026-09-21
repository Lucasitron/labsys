package com.fablab.estoque.dto;

import java.time.LocalDate;

/**
 * Evento {@code emprestimo.atrasado.event}: empréstimo com devolução vencida.
 */
public record EmprestimoAtrasadoEvent(
        Long idEmprestimo,
        Long idPessoa,
        Long idItem,
        LocalDate dataDevolucaoPrevista) {
}