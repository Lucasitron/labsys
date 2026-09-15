package com.fablab.vendas.dto;

import com.fablab.vendas.entity.Prioridade;
import com.fablab.vendas.entity.StatusTarefa;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

/**
 * Payload de criação/atualização de tarefa de marketing.
 */
public record TarefaRequest(
        @NotBlank @Size(max = 200) String titulo,
        @Size(max = 500) String descricao,
        @NotNull @NotEmpty Long idResponsavel,
        LocalDate dataInicio,
        LocalDate dataFim,
        StatusTarefa status,
        @NotNull Prioridade prioridade) {
}