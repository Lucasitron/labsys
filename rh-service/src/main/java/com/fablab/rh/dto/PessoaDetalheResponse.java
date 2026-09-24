package com.fablab.rh.dto;

import com.fablab.rh.entity.NivelAcesso;
import java.util.List;

/**
 * Detalhe agregado da pessoa (base das abas: visão geral, horas,
 * treinamentos e histórico de nível).
 *
 * @param pessoa         dados cadastrais
 * @param idFuncionario  id do funcionário vinculado (nulo quando sem vínculo)
 * @param nivel          nível de acesso atual (nulo quando sem vínculo)
 * @param departamento   departamento atual (nulo quando sem vínculo)
 * @param horasMes       horas validadas do mês atual e anterior
 * @param treinamentos   resumo de treinamentos e média
 * @param pendencias     pendências do integrante
 * @param horasPorStatus apontamentos por status
 * @param avaliacoes     avaliações de treinamento do integrante
 * @param historicoNivel timeline de evolução de nível
 */
public record PessoaDetalheResponse(
        PessoaResponse pessoa,
        Long idFuncionario,
        NivelAcesso nivel,
        String departamento,
        HorasMesResponse horasMes,
        TreinamentosResumoResponse treinamentos,
        PendenciasResponse pendencias,
        HorasPorStatusResponse horasPorStatus,
        List<AvaliacaoResponse> avaliacoes,
        List<HistoricoNivelResponse> historicoNivel) {
}
