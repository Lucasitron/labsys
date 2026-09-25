package com.fablab.vendas.service;

import com.fablab.vendas.dto.EncomendaDtos.EncomendaDetalheResponse;
import com.fablab.vendas.dto.EncomendaDtos.EncomendaListaResponse;
import com.fablab.vendas.dto.EncomendaDtos.EncomendaRequest;
import com.fablab.vendas.dto.EncomendaDtos.EncomendaResponse;
import com.fablab.vendas.dto.EncomendaDtos.HistoricoResponse;
import com.fablab.vendas.dto.EncomendaDtos.KanbanRequest;
import com.fablab.vendas.dto.EncomendaDtos.NovaOrdemRequest;
import com.fablab.vendas.dto.EncomendaDtos.StatusResponse;
import com.fablab.vendas.dto.OrcamentoDtos.ItemOrcamentoResponse;
import com.fablab.vendas.dto.VendasPrincipal;
import com.fablab.vendas.entity.Cliente;
import com.fablab.vendas.entity.Encomenda;
import com.fablab.vendas.entity.HistoricoStatusEncomenda;
import com.fablab.vendas.entity.Orcamento;
import com.fablab.vendas.entity.StatusKanban;
import com.fablab.vendas.entity.StatusOrcamento;
import com.fablab.vendas.exception.ConflitoException;
import com.fablab.vendas.exception.ResourceNotFoundException;
import com.fablab.vendas.rabbit.VendasEventPublisher;
import com.fablab.vendas.rabbit.VendasEventos.EncomendaCriadaEvent;
import com.fablab.vendas.rabbit.VendasEventos.EncomendaStatusAlteradoEvent;
import com.fablab.vendas.repository.ClienteRepository;
import com.fablab.vendas.repository.EncomendaRepository;
import com.fablab.vendas.repository.HistoricoStatusEncomendaRepository;
import com.fablab.vendas.repository.ItemOrcamentoRepository;
import com.fablab.vendas.repository.OrcamentoRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Ciclo de vida da encomenda: criação a partir de orçamento aprovado (ou venda
 * direta), Kanban com histórico e lock otimista (D-9), nova ordem por
 * alteração e enforcement criador+Admin (D-3).
 */
@Service
public class EncomendaService {

    private final EncomendaRepository encomendaRepository;
    private final OrcamentoRepository orcamentoRepository;
    private final ClienteRepository clienteRepository;
    private final ItemOrcamentoRepository itemRepository;
    private final HistoricoStatusEncomendaRepository historicoRepository;
    private final VendasEventPublisher eventPublisher;
    private final PermissaoUtil permissaoUtil;

    public EncomendaService(EncomendaRepository encomendaRepository,
                            OrcamentoRepository orcamentoRepository,
                            ClienteRepository clienteRepository,
                            ItemOrcamentoRepository itemRepository,
                            HistoricoStatusEncomendaRepository historicoRepository,
                            VendasEventPublisher eventPublisher,
                            PermissaoUtil permissaoUtil) {
        this.encomendaRepository = encomendaRepository;
        this.orcamentoRepository = orcamentoRepository;
        this.clienteRepository = clienteRepository;
        this.itemRepository = itemRepository;
        this.historicoRepository = historicoRepository;
        this.eventPublisher = eventPublisher;
        this.permissaoUtil = permissaoUtil;
    }

    @Transactional
    public EncomendaDetalheResponse criar(EncomendaRequest request, VendasPrincipal principal) {
        Encomenda encomenda = new Encomenda();
        encomenda.setDataCriacao(LocalDate.now());
        encomenda.setStatusKanban(StatusKanban.Fila);
        encomenda.setCriadoPor(principal.idPessoa());
        encomenda.setDataPrevisaoEntrega(request.dataPrevisaoEntrega());
        encomenda.setObservacoes(request.observacoes());

        if (request.idOrcamento() != null) {
            Orcamento orcamento = orcamentoRepository.findById(request.idOrcamento())
                    .orElseThrow(() -> new ResourceNotFoundException("Orçamento não encontrado"));
            if (orcamento.getStatus() != StatusOrcamento.Aprovado) {
                throw new ConflitoException("Encomenda só pode nascer de orçamento aprovado");
            }
            if (encomendaRepository.existsByIdOrcamento(orcamento.getId())) {
                throw new ConflitoException("Orçamento já convertido em encomenda");
            }
            encomenda.setIdOrcamento(orcamento.getId());
            encomenda.setIdCliente(orcamento.getIdCliente());
            encomenda.setValorFinal(orcamento.getValorTotal());
            if (request.valorFinal() != null) {
                encomenda.setValorFinal(request.valorFinal());
            }
        } else {
            if (request.clienteId() == null) {
                throw new IllegalArgumentException("Informe o orçamento aprovado ou o cliente (venda direta)");
            }
            clienteRepository.findById(request.clienteId())
                    .orElseThrow(() -> new ResourceNotFoundException("Cliente não encontrado"));
            if (request.valorFinal() == null) {
                throw new IllegalArgumentException("O valor da venda direta é obrigatório");
            }
            encomenda.setIdCliente(request.clienteId());
            encomenda.setValorFinal(request.valorFinal());
        }

        encomenda = encomendaRepository.save(encomenda);
        registrarHistorico(encomenda.getId(), null, StatusKanban.Fila.name(),
                principal.idPessoa(), "Encomenda criada na Fila");
        eventPublisher.encomendaCriada(new EncomendaCriadaEvent(encomenda.getId(),
                encomenda.getIdCliente(), encomenda.getValorFinal(), encomenda.getDataCriacao()));
        return detalhar(encomenda.getId());
    }

    @Transactional(readOnly = true)
    public EncomendaListaResponse listar(String status, Long clienteId) {
        List<Encomenda> todas = encomendaRepository.findAll();
        if (status != null && !status.isBlank()) {
            StatusKanban filtro = paraKanban(status);
            todas = todas.stream().filter(e -> e.getStatusKanban() == filtro).toList();
        }
        if (clienteId != null) {
            todas = todas.stream().filter(e -> e.getIdCliente().equals(clienteId)).toList();
        }
        List<EncomendaResponse> respostas = todas.stream().map(this::paraResponse).toList();
        Map<String, Long> counts = new LinkedHashMap<>();
        for (StatusKanban s : StatusKanban.values()) {
            counts.put(s.name(), encomendaRepository.countByStatusKanban(s));
        }
        return new EncomendaListaResponse(respostas, counts);
    }

    @Transactional(readOnly = true)
    public EncomendaDetalheResponse detalhar(Long id) {
        Encomenda encomenda = buscar(id);
        String clienteNome = clienteRepository.findById(encomenda.getIdCliente())
                .map(Cliente::getNomeRazaoSocial).orElse("—");
        List<ItemOrcamentoResponse> itens = encomenda.getIdOrcamento() == null ? List.of()
                : itemRepository.findByIdOrcamento(encomenda.getIdOrcamento()).stream()
                        .map(i -> new ItemOrcamentoResponse(i.getId(), i.getDescricao(), i.getQuantidade(),
                                i.getValorUnitario(), i.getValorUnitario().multiply(i.getQuantidade()),
                                i.getMaterialTipo(), i.getMaterialQuantidade(), i.getMaterialUnidade(),
                                i.getHoras(), i.isCompra()))
                        .toList();
        List<HistoricoResponse> historico = historicoRepository
                .findByIdEncomendaOrderByDataAlteracaoAsc(id).stream()
                .map(h -> new HistoricoResponse(h.getId(), h.getStatusAnterior(), h.getStatusNovo(),
                        h.getDataAlteracao(), h.getIdUsuario(), h.getObservacao()))
                .toList();
        return new EncomendaDetalheResponse(encomenda.getId(), encomenda.getIdOrcamento(),
                encomenda.getIdCliente(), clienteNome, encomenda.getDataCriacao(),
                encomenda.getDataPrevisaoEntrega(), encomenda.getStatusKanban().name(),
                encomenda.getValorFinal(), encomenda.getObservacoes(), origem(encomenda),
                encomenda.getVersao(), encomenda.getCriadoPor(), encomenda.getEncomendaOrigemId(),
                itens, historico);
    }

    @Transactional(readOnly = true)
    public StatusResponse status(Long id) {
        Encomenda encomenda = buscar(id);
        return new StatusResponse(encomenda.getId(), encomenda.getStatusKanban().name(),
                encomenda.getDataPrevisaoEntrega(), encomenda.getValorFinal());
    }

    /**
     * Move o cartão no Kanban (D-3: só criador ou Admin; D-9: 409 em conflito de
     * versão). Saída de {@code Entregue} é bloqueada.
     */
    @Transactional
    public EncomendaDetalheResponse moverKanban(Long id, KanbanRequest request, VendasPrincipal principal) {
        Encomenda encomenda = buscar(id);
        permissaoUtil.exigirCriadorOuAdmin(encomenda.getCriadoPor(), principal);
        if (!encomenda.getVersao().equals(request.versao())) {
            throw new ConflitoException(
                    "O cartão foi movido por outro usuário. Atualize a tela e tente novamente.");
        }
        StatusKanban destino = paraKanban(request.statusKanban());
        if (encomenda.getStatusKanban() == StatusKanban.Entregue) {
            throw new ConflitoException("Encomenda entregue não pode ser movida");
        }
        StatusKanban anterior = encomenda.getStatusKanban();
        encomenda.setStatusKanban(destino);
        encomenda = encomendaRepository.save(encomenda);
        registrarHistorico(id, anterior.name(), destino.name(), principal.idPessoa(), request.observacao());
        eventPublisher.encomendaStatusAlterado(new EncomendaStatusAlteradoEvent(id,
                anterior.name(), destino.name(), LocalDateTime.now()));
        return detalhar(id);
    }

    /**
     * Alteração de encomenda: encerra a atual (registro no histórico) e cria
     * nova ordem com novas estimativas/valor, referenciando a anterior.
     */
    @Transactional
    public EncomendaDetalheResponse novaOrdem(Long id, NovaOrdemRequest request, VendasPrincipal principal) {
        Encomenda atual = buscar(id);
        permissaoUtil.exigirCriadorOuAdmin(atual.getCriadoPor(), principal);
        registrarHistorico(id, atual.getStatusKanban().name(), atual.getStatusKanban().name(),
                principal.idPessoa(), "Encerrada por alteração — nova ordem criada");
        Encomenda nova = new Encomenda();
        nova.setIdCliente(atual.getIdCliente());
        nova.setDataCriacao(LocalDate.now());
        nova.setDataPrevisaoEntrega(request.dataPrevisaoEntrega());
        nova.setStatusKanban(StatusKanban.Fila);
        nova.setValorFinal(request.valorFinal());
        nova.setObservacoes(request.observacoes());
        nova.setCriadoPor(principal.idPessoa());
        nova.setEncomendaOrigemId(atual.getId());
        nova = encomendaRepository.save(nova);
        registrarHistorico(nova.getId(), null, StatusKanban.Fila.name(), principal.idPessoa(),
                "Nova ordem a partir da encomenda " + atual.getId());
        eventPublisher.encomendaCriada(new EncomendaCriadaEvent(nova.getId(),
                nova.getIdCliente(), nova.getValorFinal(), nova.getDataCriacao()));
        return detalhar(nova.getId());
    }

    private void registrarHistorico(Long idEncomenda, String anterior, String novo,
                                    Long idUsuario, String observacao) {
        HistoricoStatusEncomenda historico = new HistoricoStatusEncomenda();
        historico.setIdEncomenda(idEncomenda);
        historico.setStatusAnterior(anterior == null ? "—" : anterior);
        historico.setStatusNovo(novo);
        historico.setDataAlteracao(LocalDateTime.now());
        historico.setIdUsuario(idUsuario);
        historico.setObservacao(observacao);
        historicoRepository.save(historico);
    }

    private String origem(Encomenda encomenda) {
        if (encomenda.getEncomendaOrigemId() != null) {
            return "Nova ordem";
        }
        return encomenda.getIdOrcamento() != null ? "Orçamento" : "Venda direta";
    }

    private Encomenda buscar(Long id) {
        return encomendaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Encomenda não encontrada"));
    }

    private StatusKanban paraKanban(String status) {
        try {
            return StatusKanban.valueOf(status);
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException(
                    "Status do Kanban inválido (Fila, Produção, Acabamento, Pronto ou Entregue)");
        }
    }

    private EncomendaResponse paraResponse(Encomenda encomenda) {
        String clienteNome = clienteRepository.findById(encomenda.getIdCliente())
                .map(Cliente::getNomeRazaoSocial).orElse("—");
        int itens = encomenda.getIdOrcamento() == null ? 0
                : itemRepository.findByIdOrcamento(encomenda.getIdOrcamento()).size();
        return new EncomendaResponse(encomenda.getId(), encomenda.getIdOrcamento(),
                encomenda.getIdCliente(), clienteNome, encomenda.getDataCriacao(),
                encomenda.getDataPrevisaoEntrega(), encomenda.getStatusKanban().name(),
                encomenda.getValorFinal(), origem(encomenda), itens,
                encomenda.getVersao(), encomenda.getCriadoPor());
    }
}
