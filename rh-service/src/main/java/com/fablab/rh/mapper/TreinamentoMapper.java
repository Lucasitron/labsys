package com.fablab.rh.mapper;

import com.fablab.rh.dto.AvaliacaoRequest;
import com.fablab.rh.dto.AvaliacaoResponse;
import com.fablab.rh.dto.TreinamentoRequest;
import com.fablab.rh.dto.TreinamentoResponse;
import com.fablab.rh.entity.AvaliacaoTreinamento;
import com.fablab.rh.entity.Funcionario;
import com.fablab.rh.entity.Treinamento;
import java.time.LocalDate;

/**
 * Mapeia treinamentos e avaliações para DTOs.
 */
public final class TreinamentoMapper {

    private TreinamentoMapper() {
    }

    public static Treinamento toEntity(TreinamentoRequest request, Funcionario tutor) {
        Treinamento treinamento = new Treinamento();
        treinamento.setTitulo(request.titulo());
        treinamento.setDescricao(request.descricao());
        treinamento.setUrlConteudo(request.urlConteudo());
        treinamento.setTutor(tutor);
        return treinamento;
    }

    public static TreinamentoResponse toResponse(Treinamento treinamento) {
        return new TreinamentoResponse(
                treinamento.getId(),
                treinamento.getTitulo(),
                treinamento.getDescricao(),
                treinamento.getUrlConteudo(),
                treinamento.getTutor().getId());
    }

    public static AvaliacaoTreinamento toAvaliacaoEntity(Treinamento treinamento,
                                                         Funcionario aluno,
                                                         AvaliacaoRequest request) {
        AvaliacaoTreinamento avaliacao = new AvaliacaoTreinamento();
        avaliacao.setTreinamento(treinamento);
        avaliacao.setFuncionario(aluno);
        avaliacao.setNota(request.nota());
        avaliacao.setFeedback(request.feedback());
        avaliacao.setDataAvaliacao(request.dataAvaliacao() == null ? LocalDate.now() : request.dataAvaliacao());
        return avaliacao;
    }

    public static AvaliacaoResponse toAvaliacaoResponse(AvaliacaoTreinamento avaliacao) {
        return new AvaliacaoResponse(
                avaliacao.getId(),
                avaliacao.getTreinamento().getId(),
                avaliacao.getFuncionario().getId(),
                avaliacao.getFuncionario().getPessoa().getNomeCompleto(),
                avaliacao.getNota(),
                avaliacao.getFeedback(),
                avaliacao.getDataAvaliacao());
    }
}