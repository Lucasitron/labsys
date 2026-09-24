package com.fablab.producao.service;

import com.fablab.producao.dto.TarefaRequest;
import com.fablab.producao.dto.TarefaResponse;
import com.fablab.producao.dto.TarefaStatusRequest;
import com.fablab.producao.entity.PrioridadeTarefa;
import com.fablab.producao.entity.Projeto;
import com.fablab.producao.entity.Tarefa;
import com.fablab.producao.entity.TarefaStatus;
import com.fablab.producao.exception.ForbiddenException;
import com.fablab.producao.exception.ResourceNotFoundException;
import com.fablab.producao.repository.TarefaRepository;
import java.time.LocalDate;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Regras de negócio de tarefas de projeto. */
@Service
public class TarefaService {

    private final TarefaRepository tarefaRepository;
    private final ProjetoService projetoService;
    private final AcessoService acessoService;

    public TarefaService(TarefaRepository tarefaRepository,
                         ProjetoService projetoService,
                         AcessoService acessoService) {
        this.tarefaRepository = tarefaRepository;
        this.projetoService = projetoService;
        this.acessoService = acessoService;
    }

    @Transactional(readOnly = true)
    public List<TarefaResponse> listar(Long idProjeto, Long idResponsavel, TarefaStatus status) {
        List<Tarefa> tarefas;
        if (idProjeto != null) {
            tarefas = tarefaRepository.findByProjeto_IdProjeto(idProjeto);
        } else if (idResponsavel != null) {
            tarefas = tarefaRepository.findByIdResponsavel(idResponsavel);
        } else if (status != null) {
            tarefas = tarefaRepository.findByStatus(status);
        } else {
            tarefas = tarefaRepository.findAll();
        }
        return tarefas.stream().map(TarefaResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public TarefaResponse buscar(Long id) {
        return TarefaResponse.from(obter(id));
    }

    @Transactional
    public TarefaResponse criar(TarefaRequest request) {
        Projeto projeto = projetoService.obter(request.idProjeto());
        verificarResponsabilidade(projeto.getIdResponsavel());
        Tarefa tarefa = new Tarefa();
        tarefa.setProjeto(projeto);
        aplicar(tarefa, request);
        tarefa.setStatus(TarefaStatus.PENDENTE);
        return TarefaResponse.from(tarefaRepository.save(tarefa));
    }

    @Transactional
    public TarefaResponse atualizar(Long id, TarefaRequest request) {
        Tarefa tarefa = obter(id);
        verificarResponsabilidade(tarefa.getProjeto().getIdResponsavel());
        tarefa.setProjeto(projetoService.obter(request.idProjeto()));
        aplicar(tarefa, request);
        return TarefaResponse.from(tarefa);
    }

    @Transactional
    public TarefaResponse alterarStatus(Long id, TarefaStatusRequest request) {
        Tarefa tarefa = obter(id);
        verificarResponsabilidade(tarefa.getProjeto().getIdResponsavel());
        tarefa.setStatus(request.status());
        if (request.status() == TarefaStatus.CONCLUIDA) {
            tarefa.setDataConclusao(request.dataConclusao() == null ? LocalDate.now() : request.dataConclusao());
        } else {
            tarefa.setDataConclusao(request.dataConclusao());
        }
        return TarefaResponse.from(tarefa);
    }

    @Transactional
    public void remover(Long id) {
        Tarefa tarefa = obter(id);
        verificarResponsabilidade(tarefa.getProjeto().getIdResponsavel());
        tarefaRepository.delete(tarefa);
    }

    Tarefa obter(Long id) {
        return tarefaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Tarefa", id));
    }

    private void verificarResponsabilidade(Long idResponsavelProjeto) {
        if (!acessoService.podeEditar(idResponsavelProjeto)) {
            throw new ForbiddenException("Apenas o responsável pelo projeto ou um administrador pode editar suas tarefas");
        }
    }

    private void aplicar(Tarefa tarefa, TarefaRequest request) {
        tarefa.setTitulo(request.titulo());
        tarefa.setDescricao(request.descricao());
        tarefa.setIdResponsavel(request.idResponsavel());
        tarefa.setDataInicio(request.dataInicio());
        tarefa.setDataFimPrevista(request.dataFimPrevista());
        tarefa.setPrioridade(request.prioridade() == null ? PrioridadeTarefa.MEDIA : request.prioridade());
    }
}