package com.fablab.vendas.service;

import com.fablab.vendas.dto.CrmDtos.InteracaoRequest;
import com.fablab.vendas.dto.CrmDtos.InteracaoResponse;
import com.fablab.vendas.dto.CrmDtos.TarefaAtualizacaoRequest;
import com.fablab.vendas.dto.CrmDtos.TarefaListaResponse;
import com.fablab.vendas.dto.CrmDtos.TarefaRequest;
import com.fablab.vendas.dto.CrmDtos.TarefaResponse;
import com.fablab.vendas.dto.VendasPrincipal;
import com.fablab.vendas.entity.InteracaoCliente;
import com.fablab.vendas.entity.TarefaMarketing;
import com.fablab.vendas.exception.ResourceNotFoundException;
import com.fablab.vendas.repository.ClienteRepository;
import com.fablab.vendas.repository.InteracaoClienteRepository;
import com.fablab.vendas.repository.TarefaMarketingRepository;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** CRM interno: interações (timeline) e tarefas de marketing. */
@Service
public class CrmService {

    private static final Set<String> TIPOS = Set.of("E-mail", "Telefone", "Reunião", "WhatsApp");
    private static final Set<String> STATUS = Set.of("Pendente", "Em Andamento", "Concluída");
    private static final Set<String> PRIORIDADES = Set.of("Baixa", "Média", "Alta");

    private final InteracaoClienteRepository interacaoRepository;
    private final TarefaMarketingRepository tarefaRepository;
    private final ClienteRepository clienteRepository;
    private final PermissaoUtil permissaoUtil;

    public CrmService(InteracaoClienteRepository interacaoRepository,
                      TarefaMarketingRepository tarefaRepository,
                      ClienteRepository clienteRepository,
                      PermissaoUtil permissaoUtil) {
        this.interacaoRepository = interacaoRepository;
        this.tarefaRepository = tarefaRepository;
        this.clienteRepository = clienteRepository;
        this.permissaoUtil = permissaoUtil;
    }

    @Transactional
    public InteracaoResponse registrarInteracao(InteracaoRequest request, VendasPrincipal principal) {
        clienteRepository.findById(request.clienteId())
                .orElseThrow(() -> new ResourceNotFoundException("Cliente não encontrado"));
        if (!TIPOS.contains(request.tipo())) {
            throw new IllegalArgumentException(
                    "Tipo de interação inválido (E-mail, Telefone, Reunião ou WhatsApp)");
        }
        InteracaoCliente interacao = new InteracaoCliente();
        interacao.setIdCliente(request.clienteId());
        interacao.setTipo(request.tipo());
        interacao.setDescricao(request.descricao());
        interacao.setDataInteracao(
                request.dataInteracao() != null ? request.dataInteracao() : LocalDateTime.now());
        interacao.setIdUsuario(principal.idPessoa());
        interacao = interacaoRepository.save(interacao);
        return new InteracaoResponse(interacao.getId(), interacao.getIdCliente(),
                interacao.getDataInteracao(), interacao.getTipo(), interacao.getDescricao(),
                interacao.getIdUsuario());
    }

    @Transactional(readOnly = true)
    public List<InteracaoResponse> listarInteracoes(Long idCliente) {
        clienteRepository.findById(idCliente)
                .orElseThrow(() -> new ResourceNotFoundException("Cliente não encontrado"));
        return interacaoRepository.findByIdClienteOrderByDataInteracaoDesc(idCliente).stream()
                .map(i -> new InteracaoResponse(i.getId(), i.getIdCliente(), i.getDataInteracao(),
                        i.getTipo(), i.getDescricao(), i.getIdUsuario()))
                .toList();
    }

    @Transactional
    public TarefaResponse criarTarefa(TarefaRequest request, VendasPrincipal principal) {
        String status = request.status() == null ? "Pendente" : request.status();
        String prioridade = request.prioridade() == null ? "Média" : request.prioridade();
        validarTarefa(status, prioridade);
        if (request.dataInicio() != null && request.dataFim() != null
                && request.dataFim().isBefore(request.dataInicio())) {
            throw new IllegalArgumentException("O prazo não pode ser anterior ao início");
        }
        TarefaMarketing tarefa = new TarefaMarketing();
        tarefa.setTitulo(request.titulo());
        tarefa.setDescricao(request.descricao());
        tarefa.setIdResponsavel(request.responsavelId());
        tarefa.setDataInicio(request.dataInicio());
        tarefa.setDataFim(request.dataFim());
        tarefa.setStatus(status);
        tarefa.setPrioridade(prioridade);
        tarefa.setCriadoPor(principal.idPessoa());
        tarefa = tarefaRepository.save(tarefa);
        return paraTarefa(tarefa);
    }

    @Transactional(readOnly = true)
    public TarefaListaResponse listarTarefas(Long responsavelId, String status) {
        List<TarefaMarketing> tarefas = tarefaRepository.findAll();
        if (responsavelId != null) {
            tarefas = tarefas.stream().filter(t -> t.getIdResponsavel().equals(responsavelId)).toList();
        }
        if (status != null && !status.isBlank()) {
            tarefas = tarefas.stream().filter(t -> t.getStatus().equals(status)).toList();
        }
        Map<String, Long> counts = new LinkedHashMap<>();
        for (String s : STATUS) {
            counts.put(s, tarefaRepository.countByStatus(s));
        }
        return new TarefaListaResponse(tarefas.stream().map(this::paraTarefa).toList(), counts);
    }

    @Transactional
    public TarefaResponse atualizarTarefa(Long id, TarefaAtualizacaoRequest request,
                                          VendasPrincipal principal) {
        TarefaMarketing tarefa = tarefaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Tarefa não encontrada"));
        permissaoUtil.exigirCriadorOuAdmin(tarefa.getCriadoPor(), principal);
        if (request.status() != null) {
            if (!STATUS.contains(request.status())) {
                throw new IllegalArgumentException("Status inválido (Pendente, Em Andamento ou Concluída)");
            }
            tarefa.setStatus(request.status());
        }
        if (request.prioridade() != null) {
            if (!PRIORIDADES.contains(request.prioridade())) {
                throw new IllegalArgumentException("Prioridade inválida (Baixa, Média ou Alta)");
            }
            tarefa.setPrioridade(request.prioridade());
        }
        if (request.dataFim() != null) {
            tarefa.setDataFim(request.dataFim());
        }
        return paraTarefa(tarefaRepository.save(tarefa));
    }

    private void validarTarefa(String status, String prioridade) {
        if (!STATUS.contains(status)) {
            throw new IllegalArgumentException("Status inválido (Pendente, Em Andamento ou Concluída)");
        }
        if (!PRIORIDADES.contains(prioridade)) {
            throw new IllegalArgumentException("Prioridade inválida (Baixa, Média ou Alta)");
        }
    }

    private TarefaResponse paraTarefa(TarefaMarketing tarefa) {
        return new TarefaResponse(tarefa.getId(), tarefa.getTitulo(), tarefa.getDescricao(),
                tarefa.getIdResponsavel(), tarefa.getDataInicio(), tarefa.getDataFim(),
                tarefa.getStatus(), tarefa.getPrioridade(), tarefa.getCriadoPor());
    }
}
