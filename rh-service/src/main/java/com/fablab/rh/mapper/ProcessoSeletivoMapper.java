package com.fablab.rh.mapper;

import com.fablab.rh.dto.ProcessoSeletivoResponse;
import com.fablab.rh.entity.ProcessoSeletivo;

/**
 * Mapeia entidades {@code ProcessoSeletivo} para DTOs.
 */
public final class ProcessoSeletivoMapper {

    private ProcessoSeletivoMapper() {
    }

    public static ProcessoSeletivoResponse toResponse(ProcessoSeletivo processo) {
        return new ProcessoSeletivoResponse(
                processo.getId(),
                processo.getCandidato().getId(),
                processo.getCandidato().getNomeCompleto(),
                processo.getTutor().getId(),
                processo.getStatusProcesso(),
                processo.getDataInscricao(),
                processo.getResultadoFinal(),
                processo.getGrupo() == null ? null : processo.getGrupo().getId(),
                processo.getNota(),
                processo.getFeedback());
    }
}