package com.fablab.producao.service;

import com.fablab.producao.dto.HistoricoKanbanResponse;
import com.fablab.producao.dto.ItemConsumido;
import com.fablab.producao.dto.KanbanMovimentoRequest;
import com.fablab.producao.dto.KanbanRequest;
import com.fablab.producao.dto.KanbanResponse;
import com.fablab.producao.dto.KanbanStatusAlteradoEvent;
import com.fablab.producao.dto.ProducaoConcluidaEvent;
import com.fablab.producao.dto.ProducaoStatusEvent;
import com.fablab.producao.entity.ConsumoEncomenda;
import com.fablab.producao.entity.EncomendaKanban;
import com.fablab.producao.entity.HistoricoKanban;
import com.fablab.producao.entity.KanbanStatus;
import com.fablab.producao.exception.ForbiddenException;
import com.fablab.producao.exception.ResourceNotFoundException;
import com.fablab.producao.repository.ConsumoEncomendaRepository;
import com.fablab.producao.repository.EncomendaKanbanRepository;
import com.fablab.producao.repository.HistoricoKanbanRepository;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Kanban de produção. Movimenta as encomendas entre as colunas, mantém o
 * histórico e publica os eventos de sincronização (Vendas) e conclusão
 * (Estoque/Financeiro).
 */
@Service
public class KanbanService {

    private final EncomendaKanbanRepository kanbanRepository;
    private final HistoricoKanbanRepository historicoRepository;
    private final ConsumoEncomendaRepository consumoRepository;
    private final ProducaoEventPublisher eventPublisher;
    private final AcessoService acessoService;

    public KanbanService(EncomendaKanbanRepository kanbanRepository,
                         HistoricoKanbanRepository historicoRepository,
                         ConsumoEncomendaRepository consumoRepository,
                         ProducaoEventPublisher eventPublisher,
                         AcessoService acessoService) {
        this.kanbanRepository = kanbanRepository;
        this.historicoRepository = historicoRepository;
        this.consumoRepository = consumoRepository;
        this.eventPublisher = eventPublisher;
        this.acessoService = acessoService;
    }

    @Transactional(readOnly = true)
    public List<KanbanResponse> listar(KanbanStatus status) {
        List<EncomendaKanban> cartoes = status == null
                ? kanbanRepository.findAllByOrderByStatusAscOrdemAsc()
                : kanbanRepository.findByStatusOrderByOrdemAsc(status);
        return cartoes.stream().map(KanbanResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public KanbanResponse buscarPorEncomenda(Long idEncomenda) {
        return KanbanResponse.from(obterPorEncomenda(idEncomenda));
    }

    @Transactional(readOnly = true)
    public List<HistoricoKanbanResponse> historico(Long idEncomenda) {
        return historicoRepository.findByIdEncomendaOrderByDataAlteracaoDesc(idEncomenda)
                .stream().map(HistoricoKanbanResponse::from).toList();
    }

    @Transactional
    public KanbanResponse incluir(KanbanRequest request) {
        if (kanbanRepository.existsByIdEncomenda(request.idEncomenda())) {
            throw new IllegalArgumentException("Já existe um cartão no Kanban para a encomenda "
                    + request.idEncomenda());
        }
        EncomendaKanban kanban = new EncomendaKanban();
        kanban.setIdEncomenda(request.idEncomenda());
        kanban.setIdResponsavel(request.idResponsavel());
        kanban.setStatus(KanbanStatus.FILA);
        kanban.setDataEntradaStatus(LocalDateTime.now());
        kanban.setOrdem(request.ordem() == null ? 0 : request.ordem());
        kanban = kanbanRepository.save(kanban);
        registrarHistorico(kanban, null, KanbanStatus.FILA, acessoService.idUsuario(), "Cartão incluído");
        return KanbanResponse.from(kanban);
    }

    @Transactional
    public KanbanResponse mover(Long idKanban, KanbanMovimentoRequest request) {
        EncomendaKanban kanban = obter(idKanban);
        verificarResponsabilidade(kanban);
        KanbanStatus anterior = kanban.getStatus();
        kanban.setStatus(request.statusNovo());
        kanban.setDataEntradaStatus(LocalDateTime.now());

        Long idUsuario = request.idUsuario() != null ? request.idUsuario() : acessoService.idUsuario();
        LocalDateTime agora = LocalDateTime.now();
        registrarHistorico(kanban, anterior, request.statusNovo(), idUsuario, request.observacao());
        eventPublisher.publicarStatusAlterado(new ProducaoStatusEvent(
                kanban.getIdEncomenda(), request.statusNovo().name(), idUsuario, request.observacao()));
        eventPublisher.publicarKanbanStatusAlterado(new KanbanStatusAlteradoEvent(
                kanban.getIdEncomenda(), anterior, request.statusNovo(), agora));

        if (request.statusNovo() == KanbanStatus.ENTREGUE && anterior != KanbanStatus.ENTREGUE) {
            publicarConclusao(kanban);
        }
        return KanbanResponse.from(kanban);
    }

    @Transactional
    public void remover(Long idKanban) {
        EncomendaKanban kanban = obter(idKanban);
        verificarResponsabilidade(kanban);
        kanbanRepository.delete(kanban);
    }

    private void publicarConclusao(EncomendaKanban kanban) {
        List<ItemConsumido> itens = consumoRepository.findByIdEncomenda(kanban.getIdEncomenda()).stream()
                .map(this::toItemConsumido)
                .toList();
        eventPublisher.publicarProducaoConcluida(
                new ProducaoConcluidaEvent(kanban.getIdEncomenda(), null, itens, java.time.LocalDate.now()));
    }

    private ItemConsumido toItemConsumido(ConsumoEncomenda consumo) {
        return new ItemConsumido(consumo.getIdItem(), consumo.getQuantidadeConsumida());
    }

    private void registrarHistorico(EncomendaKanban kanban,
                                    KanbanStatus anterior,
                                    KanbanStatus novo,
                                    Long idUsuario,
                                    String observacao) {
        HistoricoKanban historico = new HistoricoKanban();
        historico.setIdEncomenda(kanban.getIdEncomenda());
        historico.setStatusAnterior(anterior);
        historico.setStatusNovo(novo);
        historico.setDataAlteracao(LocalDateTime.now());
        historico.setIdUsuario(idUsuario);
        historico.setObservacao(observacao);
        historicoRepository.save(historico);
    }

    EncomendaKanban obter(Long id) {
        return kanbanRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Cartão do Kanban", id));
    }

    private EncomendaKanban obterPorEncomenda(Long idEncomenda) {
        return kanbanRepository.findByIdEncomenda(idEncomenda)
                .orElseThrow(() -> new ResourceNotFoundException("Cartão do Kanban para a encomenda " + idEncomenda));
    }

    private void verificarResponsabilidade(EncomendaKanban kanban) {
        if (!acessoService.podeEditar(kanban.getIdResponsavel())) {
            throw new ForbiddenException("Apenas o responsável pelo cartão ou um administrador pode movimentá-lo");
        }
    }
}