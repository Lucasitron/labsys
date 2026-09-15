package com.fablab.vendas.service;

import com.fablab.vendas.dto.EncomendaRequest;
import com.fablab.vendas.dto.EncomendaResponse;
import com.fablab.vendas.dto.OrcamentoAprovadoEvent;
import com.fablab.vendas.dto.OrcamentoRequest;
import com.fablab.vendas.dto.OrcamentoResponse;
import com.fablab.vendas.dto.OrcamentoStatusRequest;
import com.fablab.vendas.entity.Cliente;
import com.fablab.vendas.entity.Encomenda;
import com.fablab.vendas.entity.HistoricoStatusEncomenda;
import com.fablab.vendas.entity.ItemOrcamento;
import com.fablab.vendas.entity.Orcamento;
import com.fablab.vendas.entity.StatusKanban;
import com.fablab.vendas.entity.StatusOrcamento;
import com.fablab.vendas.exception.ResourceNotFoundException;
import com.fablab.vendas.repository.ClienteRepository;
import com.fablab.vendas.repository.EncomendaRepository;
import com.fablab.vendas.repository.HistoricoStatusEncomendaRepository;
import com.fablab.vendas.repository.OrcamentoRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Criação, atualização e aprovação de orçamentos, incluindo a conversão em
 * encomenda quando aprovado.
 */
@Service
public class OrcamentoService {

    private final OrcamentoRepository orcamentoRepository;
    private final ClienteRepository clienteRepository;
    private final EncomendaRepository encomendaRepository;
    private final HistoricoStatusEncomendaRepository historicoRepository;
    private final VendasEventPublisher eventPublisher;

    public OrcamentoService(OrcamentoRepository orcamentoRepository,
                            ClienteRepository clienteRepository,
                            EncomendaRepository encomendaRepository,
                            HistoricoStatusEncomendaRepository historicoRepository,
                            VendasEventPublisher eventPublisher) {
        this.orcamentoRepository = orcamentoRepository;
        this.clienteRepository = clienteRepository;
        this.encomendaRepository = encomendaRepository;
        this.historicoRepository = historicoRepository;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public OrcamentoResponse criar(OrcamentoRequest request) {
        Cliente cliente = clienteRepository.findById(request.idCliente())
                .orElseThrow(() -> new ResourceNotFoundException("Cliente não encontrado: " + request.idCliente()));

        Orcamento orcamento = new Orcamento();
        orcamento.setCliente(cliente);
        orcamento.setDataCriacao(LocalDate.now());
        orcamento.setValidade(request.validade());
        orcamento.setStatus(StatusOrcamento.PENDENTE);
        orcamento.setObservacoes(request.observacoes());
        orcamento.setItens(new ArrayList<>());

        for (var itemReq : request.itens()) {
            ItemOrcamento item = new ItemOrcamento();
            item.setDescricao(itemReq.descricao());
            item.setQuantidade(itemReq.quantidade());
            item.setValorUnitario(itemReq.valorUnitario());
            orcamento.adicionarItem(item);
        }

        orcamento.recalcularValorTotal();
        return OrcamentoResponse.of(orcamentoRepository.save(orcamento));
    }

    @Transactional
    public OrcamentoResponse atualizar(Long id, OrcamentoRequest request) {
        Orcamento orcamento = obter(id);
        if (orcamento.getStatus() != StatusOrcamento.PENDENTE
                && orcamento.getStatus() != StatusOrcamento.AJUSTE) {
            throw new IllegalArgumentException("Apenas orçamentos pendentes ou em ajuste podem ser editados");
        }

        Cliente cliente = clienteRepository.findById(request.idCliente())
                .orElseThrow(() -> new ResourceNotFoundException("Cliente não encontrado: " + request.idCliente()));

        orcamento.setCliente(cliente);
        orcamento.setValidade(request.validade());
        orcamento.setObservacoes(request.observacoes());

        orcamento.getItens().clear();
        for (var itemReq : request.itens()) {
            ItemOrcamento item = new ItemOrcamento();
            item.setDescricao(itemReq.descricao());
            item.setQuantidade(itemReq.quantidade());
            item.setValorUnitario(itemReq.valorUnitario());
            orcamento.adicionarItem(item);
        }

        orcamento.recalcularValorTotal();
        return OrcamentoResponse.of(orcamentoRepository.save(orcamento));
    }

    @Transactional
    public OrcamentoResponse mudarStatus(Long id, OrcamentoStatusRequest request) {
        Orcamento orcamento = obter(id);
        orcamento.setStatus(request.status());
        orcamento = orcamentoRepository.save(orcamento);

        if (request.status() == StatusOrcamento.APROVADO) {
            eventPublisher.publishOrcamentoAprovado(new OrcamentoAprovadoEvent(
                    orcamento.getId(),
                    orcamento.getCliente().getId(),
                    orcamento.getValorTotal(),
                    LocalDate.now()));
        }

        return OrcamentoResponse.of(orcamento);
    }

    @Transactional
    public EncomendaResponse converterParaEncomenda(Long idOrcamento, EncomendaRequest request) {
        Orcamento orcamento = obter(idOrcamento);
        if (orcamento.getStatus() != StatusOrcamento.APROVADO) {
            throw new IllegalArgumentException("Apenas orçamentos aprovados podem ser convertidos em encomenda");
        }

        Cliente cliente = clienteRepository.findById(request.idCliente())
                .orElseThrow(() -> new ResourceNotFoundException("Cliente não encontrado: " + request.idCliente()));

        Encomenda encomenda = new Encomenda();
        encomenda.setOrcamento(orcamento);
        encomenda.setCliente(cliente);
        encomenda.setDataCriacao(LocalDate.now());
        encomenda.setDataPrevisaoEntrega(
                request.dataPrevisaoEntrega() != null ? request.dataPrevisaoEntrega() : orcamento.getValidade());
        encomenda.setStatusKanban(StatusKanban.FILA);
        encomenda.setValorFinal(
                request.valorFinal() != null ? request.valorFinal() : orcamento.getValorTotal());
        encomenda.setObservacoes(request.observacoes());

        HistoricoStatusEncomenda historico = new HistoricoStatusEncomenda();
        historico.setStatusAnterior(null);
        historico.setStatusNovo(StatusKanban.FILA);
        historico.setDataAlteracao(LocalDateTime.now());
        encomenda.adicionarHistorico(historico);

        encomenda = encomendaRepository.save(encomenda);

        eventPublisher.publishEncomendaCriada(new com.fablab.vendas.dto.EncomendaCriadaEvent(
                encomenda.getId(), cliente.getId(),
                encomenda.getStatusKanban().name(),
                encomenda.getValorFinal(), encomenda.getDataCriacao()));

        return EncomendaResponse.of(encomenda);
    }

    private Orcamento obter(Long id) {
        return orcamentoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Orçamento não encontrado: " + id));
    }
}