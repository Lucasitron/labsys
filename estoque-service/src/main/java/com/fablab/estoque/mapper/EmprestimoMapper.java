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
                rotuloPessoa(emprestimo.getIdPessoa()),
                emprestimo.getQuantidade(),
                emprestimo.getDataEmprestimo(),
                emprestimo.getDataDevolucaoPrevista(),
                emprestimo.getDataDevolucaoReal(),
                emprestimo.getStatus(),
                emprestimo.getObservacao(),
                emprestimo.getResponsavel());
    }

    /**
     * Rótulo do tomador derivado do vínculo existente (E-4/D-4).
     *
     * <p>O nome oficial vive no Pessoas &amp; RH (integração futura); sem nova
     * tabela, expõe o identificador como rótulo estável.</p>
     */
    public static String rotuloPessoa(Long idPessoa) {
        return idPessoa == null ? null : "Pessoa #" + idPessoa;
    }
}