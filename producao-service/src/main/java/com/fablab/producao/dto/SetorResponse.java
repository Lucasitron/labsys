package com.fablab.producao.dto;

import com.fablab.producao.entity.Setor;
import java.util.List;

/** Representação detalhada de um setor 5S. */
public record SetorResponse(
        Long idSetor,
        Integer numero,
        String nome,
        String descricao,
        String observacoes,
        String fotoCorretoUrl,
        String fotoIncorretoUrl,
        Boolean ativo,
        List<MaterialSetorResponse> materiais,
        List<SinalizacaoSetorResponse> sinalizacoes,
        List<ChecklistItemResponse> checklists,
        List<ResponsavelSetorResponse> responsaveis) {

    public static SetorResponse of(Setor setor,
                                   List<MaterialSetorResponse> materiais,
                                   List<SinalizacaoSetorResponse> sinalizacoes,
                                   List<ChecklistItemResponse> checklists,
                                   List<ResponsavelSetorResponse> responsaveis) {
        return new SetorResponse(
                setor.getIdSetor(),
                setor.getNumero(),
                setor.getNome(),
                setor.getDescricao(),
                setor.getObservacoes(),
                setor.getFotoCorretoUrl(),
                setor.getFotoIncorretoUrl(),
                setor.getAtivo(),
                materiais,
                sinalizacoes,
                checklists,
                responsaveis);
    }
}