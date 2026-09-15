package com.fablab.vendas.service;

import com.fablab.vendas.dto.TarefaRequest;
import com.fablab.vendas.dto.TarefaResponse;
import com.fablab.vendas.entity.StatusTarefa;
import com.fablab.vendas.entity.TarefaMarketing;
import com.fablab.vendas.exception.ResourceNotFoundException;
import com.fablab.vendas.repository.TarefaMarketingRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Tarefas internas de marketing.
 */
@Service
public class TarefaService {

    private final TarefaMarketingRepository tarefaRepository;

    public TarefaService(TarefaMarketingRepository tarefaRepository) {
        this.tarefaRepository = tarefaRepository;
    }

    @Transactional
    public TarefaResponse criar(TarefaRequest request) {
        TarefaMarketing tarefa = new TarefaMarketing();
        preencher(tarefa, request, StatusTarefa.PENDENTE);
        return TarefaResponse.of(tarefaRepository.save(tarefa));
    }

    @Transactional(readOnly = true)
    public List<TarefaResponse> listar(Long idResponsavel, StatusTarefa status) {
        List<TarefaMarketing> tarefas;
        if (idResponsavel != null) {
            tarefas = tarefaRepository.findByIdResponsavel(idResponsavel);
        } else if (status != null) {
            tarefas = tarefaRepository.findByStatus(status);
        } else {
            tarefas = tarefaRepository.findAll();
        }
        return tarefas.stream().map(TarefaResponse::of).toList();
    }

    @Transactional
    public TarefaResponse atualizar(Long id, TarefaRequest request) {
        TarefaMarketing tarefa = obter(id);
        preencher(tarefa, request, request.status() != null ? request.status() : tarefa.getStatus());
        return TarefaResponse.of(tarefaRepository.save(tarefa));
    }

    @Transactional
    public TarefaResponse atualizarStatus(Long id, StatusTarefa status) {
        TarefaMarketing tarefa = obter(id);
        tarefa.setStatus(status);
        return TarefaResponse.of(tarefaRepository.save(tarefa));
    }

    private TarefaMarketing obter(Long id) {
        return tarefaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Tarefa não encontrada: " + id));
    }

    private void preencher(TarefaMarketing tarefa, TarefaRequest request, StatusTarefa status) {
        tarefa.setTitulo(request.titulo());
        tarefa.setDescricao(request.descricao());
        tarefa.setIdResponsavel(request.idResponsavel());
        tarefa.setDataInicio(request.dataInicio());
        tarefa.setDataFim(request.dataFim());
        tarefa.setStatus(status);
        tarefa.setPrioridade(request.prioridade());
    }
}