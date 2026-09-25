package com.fablab.estoque.dto;

import com.fablab.estoque.entity.StatusEmprestimo;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Resposta de empréstimo.
 */
public record EmprestimoResponse(
        Long id,
        Long idItem,
        String nomeItem,
        Long idPessoa,
        String pessoa,
        BigDecimal quantidade,
        LocalDate dataEmprestimo,
        LocalDate dataDevolucaoPrevista,
        LocalDate dataDevolucaoReal,
        StatusEmprestimo status,
        String observacao,
        String responsavel) {
}