package com.fablab.vendas.service;

import com.fablab.vendas.config.RabbitMqConfig;
import com.fablab.vendas.dto.EncomendaCriadaEvent;
import com.fablab.vendas.dto.EncomendaEntregueEvent;
import com.fablab.vendas.dto.EncomendaRequest;
import com.fablab.vendas.dto.EncomendaResponse;
import com.fablab.vendas.dto.HistoricoResponse;
import com.fablab.vendas.dto.KanbanRequest;
import com.fablab.vendas.dto.KanbanStatusEvent;
import com.fablab.vendas.dto.ProducaoStatusEvent;
import com.fablab.vendas.entity.Cliente;
import com.fablab.vendas.entity.Encomenda;
import com.fablab.vendas.entity.HistoricoStatusEncomenda;
import com.fablab.vendas.entity.Orcamento;
import com.fablab.vendas.entity.StatusKanban;
import com.fablab.vendas.exception.ResourceNotFoundException;
import com.fablab.vendas.repository.ClienteRepository;
import com.fablab.vendas.repository.EncomendaRepository;
import com.fablab.vendas.repository.HistoricoStatusEncomendaRepository;
import com.fablab.vendas.repository.OrcamentoRepository;
import jakarta.persistence.OptimisticLockException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Ciclo de vida das encomendas e o Kanban de status com controle otimista de
 * concorrência e auditoria das movimentações.
 */
@Service
public class EncomendaService {

    private final EncomendaRepository encomendaRepository;
    private final ClienteRepository clienteRepository;
    private final OrcamentoRepository orcamentoRepository;
    private final HistoricoStatusEncomendaRepository historicoRepository;
    private final VendasEventPublisher eventPublisher;

    public EncomendaService(EncomendaRepository encomendaRepository,
                            ClienteRepository clienteRepository,
                            OrcamentoRepository orcamentoRepository,
                            HistoricoStatusEncomendaRepository historicoRepository,
                            VendasEventPublisher eventPublisher) {
        this.encomendaRepository = encomendaRepository;
        this.clienteRepository = clienteRepository;
        this.orcamentoRepository = orcamentoRepository;
        this.historicoRepository = historicoRepository;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public EncomendaResponse criar(EncomendaRequest request) {
        Cliente cliente = clienteRepository.findById(request.idCliente())
                .orElseThrow(() -> new ResourceNotFoundException("Cliente não encontrado: " + request.idCliente()));

        Encomenda encomenda = new Encomenda();
        if (request.idOrcamento() != null) {
            Orcamento orcamento = orcamentoRepository.findById(request.idOrcamento())
                    .orElseThrow(() -> new ResourceNotFoundException("Orçamento não encontrado: " + request.idOrcamento()));
            if (orcamento.getStatus() != com.fablab.vendas.entity.StatusOrcamento.APROVADO) {
                throw new IllegalArgumentException("Apenas orçamentos aprovados podem gerar encomendas");
            }
            encomenda.setOrcamento(orcamento);
        }
        encomenda.setCliente(cliente);
        encomenda.setDataCriacao(LocalDate.now());
        encomenda.setDataPrevisaoEntrega(request.dataPrevisaoEntrega());
        encomenda.setStatusKanban(StatusKanban.FILA);
        encomenda.setValorFinal(request.valorFinal() != null ? request.valorFinal() : java.math.BigDecimal.ZERO);
        encomenda.setObservacoes(request.observacoes());

        HistoricoStatusEncomenda historico = new HistoricoStatusEncomenda();
        historico.setStatusAnterior(null);
        historico.setStatusNovo(StatusKanban.FILA);
        historico.setDataAlteracao(LocalDateTime.now());
        encomenda.adicionarHistorico(historico);

        encomenda = encomendaRepository.save(encomenda);
        eventPublisher.publishEncomendaCriada(new EncomendaCriadaEvent(
                encomenda.getId(), cliente.getId(),
                encomenda.getStatusKanban().name(),
                encomenda.getValorFinal(), encomenda.getDataCriacao()));
        return EncomendaResponse.of(encomenda);
    }

    @Transactional
    public EncomendaResponse moverKanban(Long id, KanbanRequest request) {
        Encomenda encomenda = obter(id);
        StatusKanban anterior = encomenda.getStatusKanban();
        StatusKanban novo = request.novoStatus();

        if (!movimentoValido(anterior, novo)) {
            throw new IllegalArgumentException(
                    "Movimento inválido de " + anterior + " para " + novo);
        }

        encomenda.setStatusKanban(novo);
        HistoricoStatusEncomenda historico = new HistoricoStatusEncomenda();
        historico.setStatusAnterior(anterior);
        historico.setStatusNovo(novo);
        historico.setDataAlteracao(LocalDateTime.now());
        historico.setIdUsuario(request.idUsuario());
        historico.setObservacao(request.observacao());
        encomenda.adicionarHistorico(historico);

        try {
            encomenda = encomendaRepository.saveAndFlush(encomenda);
        } catch (OptimisticLockException ex) {
            throw new IllegalArgumentException("Encomenda foi alterada por outro usuário, tente novamente");
        }

        eventPublisher.publishKanbanStatus(new KanbanStatusEvent(
                encomenda.getId(), encomenda.getCliente().getId(),
                anterior.name(), novo.name(), LocalDateTime.now()));

        if (novo == StatusKanban.ENTREGUE) {
            eventPublisher.publishEncomendaEntregue(new EncomendaEntregueEvent(
                    encomenda.getId(), encomenda.getCliente().getId(),
                    encomenda.getValorFinal(), LocalDate.now()));
        }

        return EncomendaResponse.of(encomenda);
    }

    @Transactional
    public EncomendaResponse receberAtualizacaoProducao(ProducaoStatusEvent event) {
        Encomenda encomenda = obter(event.idEncomenda());
        StatusKanban anterior = encomenda.getStatusKanban();
        StatusKanban novo = StatusKanban.valueOf(event.statusNovo());

        if (!movimentoValido(anterior, novo)) {
            throw new IllegalArgumentException(
                    "Movimento inválido de " + anterior + " para " + novo);
        }

        encomenda.setStatusKanban(novo);
        HistoricoStatusEncomenda historico = new HistoricoStatusEncomenda();
        historico.setStatusAnterior(anterior);
        historico.setStatusNovo(novo);
        historico.setDataAlteracao(LocalDateTime.now());
        historico.setIdUsuario(event.idUsuario());
        historico.setObservacao(event.observacao() != null
                ? event.observacao() : "Atualizado via Produção & Projetos");
        encomenda.adicionarHistorico(historico);
        encomenda = encomendaRepository.save(encomenda);

        eventPublisher.publishKanbanStatus(new KanbanStatusEvent(
                encomenda.getId(), encomenda.getCliente().getId(),
                anterior.name(), novo.name(), LocalDateTime.now()));
        return EncomendaResponse.of(encomenda);
    }

    @Transactional(readOnly = true)
    public List<EncomendaResponse> listar(StatusKanban statusKanban, Long idCliente, LocalDate dataInicio,
                                          LocalDate dataFim) {
        List<Encomenda> encomendas;
        if (idCliente != null) {
            encomendas = encomendaRepository.findByCliente_Id(idCliente);
        } else if (statusKanban != null) {
            encomendas = encomendaRepository.findByStatusKanban(statusKanban);
        } else if (dataInicio != null && dataFim != null) {
            encomendas = encomendaRepository.findByDataCriacaoBetween(dataInicio, dataFim);
        } else {
            encomendas = encomendaRepository.findAll();
        }
        return encomendas.stream().map(EncomendaResponse::of).toList();
    }

    @Transactional(readOnly = true)
    public EncomendaResponse buscar(Long id) {
        return EncomendaResponse.of(obter(id));
    }

    @Transactional(readOnly = true)
    public List<HistoricoResponse> listarHistorico(Long idEncomenda) {
        return historicoRepository.findByEncomenda_IdOrderByDataAlteracaoDesc(idEncomenda)
                .stream().map(HistoricoResponse::of).toList();
    }

    private Encomenda obter(Long id) {
        return encomendaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Encomenda não encontrada: " + id));
    }

    /** Kanban permite avanço passo a passo ou entrega direta a partir da Produção. */
    private boolean movimentoValido(StatusKanban anterior, StatusKanban novo) {
        if (anterior == novo) {
            return false;
        }
        return switch (anterior) {
            case FILA -> novo == StatusKanban.PRODUCAO;
            case PRODUCAO -> novo == StatusKanban.ACABAMENTO || novo == StatusKanban.PRONTO
                    || novo == StatusKanban.ENTREGUE;
            case ACABAMENTO -> novo == StatusKanban.PRONTO || novo == StatusKanban.ENTREGUE;
            case PRONTO -> novo == StatusKanban.ENTREGUE;
            case ENTREGUE -> false;
        };
    }
}