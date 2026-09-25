package com.fablab.vendas.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/** DTOs de solicitações de edição (contrato mínimo — D-4 REPORTADO). */
public final class SolicitacaoDtos {

    private SolicitacaoDtos() {
    }

    public record SolicitacaoRequest(
            @NotNull(message = "O tipo da solicitação é obrigatório")
            String tipo,

            @NotNull(message = "O tipo do alvo é obrigatório (CLIENTE, ORCAMENTO ou ENCOMENDA)")
            String alvoTipo,

            @NotNull(message = "O id do alvo é obrigatório")
            Long alvoId,

            @NotBlank(message = "O campo é obrigatório")
            String campo,

            String valorAtual,

            @NotBlank(message = "O valor proposto é obrigatório")
            String valorProposto,

            @NotBlank(message = "A justificativa é obrigatória")
            String justificativa) {
    }

    public record DecisaoRequest(
            @NotNull(message = "A decisão é obrigatória (APROVAR ou REJEITAR)")
            String decisao,

            String motivo) {
    }

    public record SolicitacaoResponse(
            Long id,
            String tipo,
            String alvoTipo,
            Long alvoId,
            String campo,
            String valorAtual,
            String valorProposto,
            String justificativa,
            String status,
            Long solicitanteId,
            Long decididoPor,
            String motivoDecisao,
            LocalDateTime dataCriacao,
            LocalDateTime dataDecisao) {
    }

    public record SolicitacaoListaResponse(
            List<SolicitacaoResponse> solicitacoes,
            Map<String, Long> counts) {
    }
}
