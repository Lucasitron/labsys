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
        return toResponse(emprestimo, rotuloPessoa(emprestimo.getIdPessoa()));
    }

    /**
     * Mapeia com o nome do tomador já resolvido (E-4: nome oficial do RH;
     * fallback {@code Pessoa #id} quando o RH não responde).
     */
    public static EmprestimoResponse toResponse(Emprestimo emprestimo, String pessoa) {
        return new EmprestimoResponse(
                emprestimo.getId(),
                emprestimo.getItem().getId(),
                emprestimo.getItem().getNome(),
                emprestimo.getIdPessoa(),
                pessoa,
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