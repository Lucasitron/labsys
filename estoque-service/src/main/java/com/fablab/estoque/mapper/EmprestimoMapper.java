package com.fablab.estoque.mapper;

import com.fablab.estoque.dto.EmprestimoResponse;
import com.fablab.estoque.entity.Emprestimo;

/**
 * Mapeia entidades {@code Emprestimo} para DTOs.
 */
public final class EmprestimoMapper {

    private EmprestimoMapper() {
    }

    public static EmprestimoResponse toResponse(Emprestimo emprestimo) {
        return new EmprestimoResponse(
                emprestimo.getId(),
                emprestimo.getItem().getId(),
                emprestimo.getItem().getNome(),
                emprestimo.getIdPessoa(),
                emprestimo.getQuantidade(),
                emprestimo.getDataEmprestimo(),
                emprestimo.getDataDevolucaoPrevista(),
                emprestimo.getDataDevolucaoReal(),
                emprestimo.getStatus(),
                emprestimo.getObservacao());
    }
}