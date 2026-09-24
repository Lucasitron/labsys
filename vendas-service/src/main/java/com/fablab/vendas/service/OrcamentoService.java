package com.fablab.vendas.service;

import com.fablab.vendas.dto.OrcamentoDtos.ItemOrcamentoDto;
import com.fablab.vendas.dto.OrcamentoDtos.ItemOrcamentoResponse;
import com.fablab.vendas.dto.OrcamentoDtos.OrcamentoAtualizacaoRequest;
import com.fablab.vendas.dto.OrcamentoDtos.OrcamentoDetalheResponse;
import com.fablab.vendas.dto.OrcamentoDtos.OrcamentoListaResponse;
import com.fablab.vendas.dto.OrcamentoDtos.OrcamentoRequest;
import com.fablab.vendas.dto.OrcamentoDtos.OrcamentoResponse;
import com.fablab.vendas.dto.VendasPrincipal;
import com.fablab.vendas.entity.Cliente;
import com.fablab.vendas.entity.ItemOrcamento;
import com.fablab.vendas.entity.Orcamento;
import com.fablab.vendas.entity.StatusOrcamento;
import com.fablab.vendas.exception.ConflitoException;
import com.fablab.vendas.exception.ResourceNotFoundException;
import com.fablab.vendas.rabbit.VendasEventPublisher;
import com.fablab.vendas.rabbit.VendasEventos.OrcamentoAprovadoEvent;
import com.fablab.vendas.repository.ClienteRepository;
import com.fablab.vendas.repository.EncomendaRepository;
import com.fablab.vendas.repository.ItemOrcamentoRepository;
import com.fablab.vendas.repository.OrcamentoRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * CRUD de orçamentos com itens, fluxo de ajustes, aprovação
 * ({@code orcamento.aprovado.event}) e duplicação (D-2 confirmado).
 */
@Service
public class OrcamentoService {

    private final OrcamentoRepository orcamentoRepository;
    private final ItemOrcamentoRepository itemRepository;
    private final ClienteRepository clienteRepository;
    private final EncomendaRepository encomendaRepository;
    private final VendasEventPublisher eventPublisher;
    private final PermissaoUtil permissaoUtil;

    public OrcamentoService(OrcamentoRepository orcamentoRepository,
                            ItemOrcamentoRepository itemRepository,
                            ClienteRepository clienteRepository,
                            EncomendaRepository encomendaRepository,
                            VendasEventPublisher eventPublisher,
                            PermissaoUtil permissaoUtil) {
        this.orcamentoRepository = orcamentoRepository;
        this.itemRepository = itemRepository;
        this.clienteRepository = clienteRepository;
        this.encomendaRepository = encomendaRepository;
        this.eventPublisher = eventPublisher;
        this.permissaoUtil = permissaoUtil;
    }

    @Transactional
    public OrcamentoDetalheResponse criar(OrcamentoRequest request, VendasPrincipal principal) {
        Cliente cliente = clienteRepository.findById(request.clienteId())
                .orElseThrow(() -> new ResourceNotFoundException("Cliente não encontrado"));
        Orcamento orcamento = new Orcamento();
        orcamento.setIdCliente(cliente.getId());
        orcamento.setDataCriacao(LocalDate.now());
        orcamento.setValidade(request.validade());
        orcamento.setObservacoes(request.observacoes());
        orcamento.setStatus(StatusOrcamento.Pendente);
        orcamento.setValorTotal(BigDecimal.ZERO);
        orcamento.setCriadoPor(principal.idPessoa());
        orcamento = orcamentoRepository.save(orcamento);
        salvarItens(orcamento.getId(), request.itens());
        orcamento.setValorTotal(calcularTotal(itemRepository.findByIdOrcamento(orcamento.getId())));
        return detalhar(orcamentoRepository.save(orcamento).getId());
    }

    @Transactional(readOnly = true)
    public OrcamentoListaResponse listar(String status, Long clienteId) {
        List<Orcamento> todos = orcamentoRepository.findAll();
        if (status != null && !status.isBlank()) {
            StatusOrcamento filtro = paraStatus(status);
            todos = todos.stream().filter(o -> o.getStatus() == filtro).toList();
        }
        if (clienteId != null) {
            todos = todos.stream().filter(o -> o.getIdCliente().equals(clienteId)).toList();
        }
        List<OrcamentoResponse> respostas = todos.stream().map(this::paraResponse).toList();
        Map<String, Long> counts = new LinkedHashMap<>();
        for (StatusOrcamento s : StatusOrcamento.values()) {
            counts.put(s.name(), orcamentoRepository.countByStatus(s));
        }
        return new OrcamentoListaResponse(respostas, counts);
    }

    @Transactional(readOnly = true)
    public OrcamentoDetalheResponse detalhar(Long id) {
        Orcamento orcamento = buscar(id);
        List<ItemOrcamentoResponse> itens = itemRepository.findByIdOrcamento(id).stream()
                .map(this::paraItemResponse).toList();
        String clienteNome = clienteRepository.findById(orcamento.getIdCliente())
                .map(Cliente::getNomeRazaoSocial).orElse("—");
        return new OrcamentoDetalheResponse(orcamento.getId(), orcamento.getIdCliente(), clienteNome,
                orcamento.getDataCriacao(), orcamento.getValidade(), orcamento.getValorTotal(),
                orcamento.getStatus().name(), orcamento.getObservacoes(), orcamento.getCriadoPor(), itens);
    }

    /**
     * Ajustes: edita itens/valores; troca de status publica
     * {@code orcamento.aprovado.event} quando aprovado.
     */
    @Transactional
    public OrcamentoDetalheResponse atualizar(Long id, OrcamentoAtualizacaoRequest request,
                                              VendasPrincipal principal) {
        Orcamento orcamento = buscar(id);
        permissaoUtil.exigirCriadorOuAdmin(orcamento.getCriadoPor(), principal);
        if (orcamento.getStatus() == StatusOrcamento.Aprovado
                && encomendaRepository.existsByIdOrcamento(id)) {
            throw new ConflitoException("Orçamento aprovado já convertido em encomenda e não pode ser editado");
        }
        if (request.status() != null && !request.status().isBlank()) {
            StatusOrcamento novo = paraStatus(request.status());
            if (novo == StatusOrcamento.Aprovado) {
                aprovar(orcamento);
            } else {
                orcamento.setStatus(novo);
            }
        } else if (request.itens() != null && orcamento.getStatus() == StatusOrcamento.Pendente) {
            orcamento.setStatus(StatusOrcamento.Ajuste);
        }
        if (request.validade() != null) {
            orcamento.setValidade(request.validade());
        }
        if (request.observacoes() != null) {
            orcamento.setObservacoes(request.observacoes());
        }
        if (request.itens() != null) {
            itemRepository.deleteByIdOrcamento(id);
            salvarItens(id, request.itens());
            orcamento.setValorTotal(calcularTotal(itemRepository.findByIdOrcamento(id)));
        }
        return detalhar(orcamentoRepository.save(orcamento).getId());
    }

    /** D-2 confirmado: duplica orçamento (novo registro Pendente com os mesmos itens). */
    @Transactional
    public OrcamentoDetalheResponse duplicar(Long id, VendasPrincipal principal) {
        Orcamento origem = buscar(id);
        List<ItemOrcamento> itensOrigem = itemRepository.findByIdOrcamento(id);
        Orcamento copia = new Orcamento();
        copia.setIdCliente(origem.getIdCliente());
        copia.setDataCriacao(LocalDate.now());
        copia.setValidade(origem.getValidade());
        copia.setObservacoes(origem.getObservacoes());
        copia.setStatus(StatusOrcamento.Pendente);
        copia.setValorTotal(origem.getValorTotal());
        copia.setCriadoPor(principal.idPessoa());
        copia = orcamentoRepository.save(copia);
        for (ItemOrcamento item : itensOrigem) {
            ItemOrcamento novo = new ItemOrcamento();
            novo.setIdOrcamento(copia.getId());
            novo.setDescricao(item.getDescricao());
            novo.setQuantidade(item.getQuantidade());
            novo.setValorUnitario(item.getValorUnitario());
            novo.setMaterialTipo(item.getMaterialTipo());
            novo.setMaterialQuantidade(item.getMaterialQuantidade());
            novo.setMaterialUnidade(item.getMaterialUnidade());
            novo.setHoras(item.getHoras());
            novo.setCompra(item.isCompra());
            itemRepository.save(novo);
        }
        return detalhar(copia.getId());
    }

    private void aprovar(Orcamento orcamento) {
        orcamento.setStatus(StatusOrcamento.Aprovado);
        orcamentoRepository.save(orcamento);
        eventPublisher.orcamentoAprovado(new OrcamentoAprovadoEvent(
                orcamento.getId(), orcamento.getIdCliente(), orcamento.getValorTotal()));
    }

    private void salvarItens(Long idOrcamento, List<ItemOrcamentoDto> itens) {
        for (ItemOrcamentoDto dto : itens) {
            ItemOrcamento item = new ItemOrcamento();
            item.setIdOrcamento(idOrcamento);
            item.setDescricao(dto.descricao());
            item.setQuantidade(dto.quantidade());
            item.setValorUnitario(dto.valorUnitario());
            item.setMaterialTipo(dto.materialTipo());
            item.setMaterialQuantidade(dto.materialQuantidade());
            item.setMaterialUnidade(dto.materialUnidade());
            item.setHoras(dto.horas());
            item.setCompra(dto.compra() != null && dto.compra());
            itemRepository.save(item);
        }
    }

    /** Valor total calculado a partir dos itens (quantidade × valor unitário). */
    public BigDecimal calcularTotal(List<ItemOrcamento> itens) {
        return itens.stream()
                .map(i -> i.getValorUnitario().multiply(i.getQuantidade()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private Orcamento buscar(Long id) {
        return orcamentoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Orçamento não encontrado"));
    }

    private StatusOrcamento paraStatus(String status) {
        try {
            return StatusOrcamento.valueOf(status);
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("Status de orçamento inválido (Pendente, Aprovado, Recusado ou Ajuste)");
        }
    }

    private OrcamentoResponse paraResponse(Orcamento orcamento) {
        String clienteNome = clienteRepository.findById(orcamento.getIdCliente())
                .map(Cliente::getNomeRazaoSocial).orElse("—");
        int qtd = itemRepository.findByIdOrcamento(orcamento.getId()).size();
        return new OrcamentoResponse(orcamento.getId(), orcamento.getIdCliente(), clienteNome,
                orcamento.getDataCriacao(), orcamento.getValidade(), orcamento.getValorTotal(),
                orcamento.getStatus().name(), qtd, orcamento.getCriadoPor());
    }

    private ItemOrcamentoResponse paraItemResponse(ItemOrcamento item) {
        BigDecimal subtotal = item.getValorUnitario().multiply(item.getQuantidade());
        return new ItemOrcamentoResponse(item.getId(), item.getDescricao(), item.getQuantidade(),
                item.getValorUnitario(), subtotal, item.getMaterialTipo(), item.getMaterialQuantidade(),
                item.getMaterialUnidade(), item.getHoras(), item.isCompra());
    }
}
