package com.fablab.vendas.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/** DTOs de CRM interno: interações e tarefas de marketing. */
public final class CrmDtos {

    private CrmDtos() {
    }

    public record InteracaoRequest(
            @NotNull(message = "O cliente é obrigatório")
            Long clienteId,

            @NotBlank(message = "O tipo da interação é obrigatório (E-mail, Telefone, Reunião ou WhatsApp)")
            String tipo,

            @NotBlank(message = "A descrição da interação é obrigatória")
            String descricao,

            LocalDateTime dataInteracao) {
    }

    public record InteracaoResponse(
            Long id,
            Long clienteId,
            LocalDateTime dataInteracao,
            String tipo,
            String descricao,
            Long idUsuario) {
    }

    public record TarefaRequest(
            @NotBlank(message = "O título da tarefa é obrigatório")
            String titulo,

            String descricao,

            @NotNull(message = "O responsável é obrigatório")
            Long responsavelId,

            LocalDate dataInicio,
            LocalDate dataFim,

            String status,

            String prioridade) {
    }

    public record TarefaAtualizacaoRequest(
            String status,
            String prioridade,
            LocalDate dataFim) {
    }

    public record TarefaResponse(
            Long id,
            String titulo,
            String descricao,
            Long responsavelId,
            LocalDate dataInicio,
            LocalDate dataFim,
            String status,
            String prioridade,
            Long criadoPor) {
    }

    public record TarefaListaResponse(
            List<TarefaResponse> tarefas,
            Map<String, Long> counts) {
    }
}
