package com.fablab.rh.dto;

import com.fablab.rh.entity.StatusProcesso;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Resposta com os dados de um processo seletivo.
 */
public record ProcessoSeletivoResponse(
        Long id,
        Long idCandidato,
        String nomeCandidato,
        Long idTutor,
        StatusProcesso statusProcesso,
        LocalDate dataInscricao,
        String resultadoFinal,
        Long idGrupo,
        BigDecimal nota,
        String feedback) {
}
