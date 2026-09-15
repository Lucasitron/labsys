package com.fablab.rh.dto;

import com.fablab.rh.entity.StatusProcesso;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Atualização do status de um processo seletivo.
 *
 * @param statusProcesso novo status do processo
 * @param resultadoFinal resultado final (ex: aprovado/reprovado)
 */
public record ProcessoSeletivoStatusRequest(
        @NotNull(message = "statusProcesso é obrigatório")
        StatusProcesso statusProcesso,

        @Size(max = 255, message = "resultadoFinal deve ter no máximo 255 caracteres")
        String resultadoFinal) {
}