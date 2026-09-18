package com.fablab.rh.dto;

/**
 * Decisão (aprovar/rejeitar) de uma solicitação de certificado.
 *
 * @param observacao observação/motivo registrado pelo Admin
 */
public record DecisaoSolicitacaoRequest(
        String observacao) {
}