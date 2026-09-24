package com.fablab.estoque.service;

import com.fablab.estoque.dto.EmprestimoRequest;
import com.fablab.estoque.dto.EmprestimoResponse;
import com.fablab.estoque.dto.EstoquePrincipal;
import com.fablab.estoque.entity.Emprestimo;
import com.fablab.estoque.entity.Item;
import com.fablab.estoque.entity.SaidaEstoque;
import com.fablab.estoque.entity.StatusEmprestimo;
import com.fablab.estoque.entity.TipoSaida;
import com.fablab.estoque.exception.ForbiddenException;
import com.fablab.estoque.exception.ResourceNotFoundException;
import com.fablab.estoque.exception.SaldoInsuficienteException;
import com.fablab.estoque.mapper.EmprestimoMapper;
import com.fablab.estoque.rabbit.EstoqueEventPublisher;
import com.fablab.estoque.repository.EmprestimoRepository;
import com.fablab.estoque.repository.ItemRepository;
import com.fablab.estoque.repository.SaidaEstoqueRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Gestão de empréstimos de equipamentos/ferramentas.
 */
@Service
public class EmprestimoService {

    private static final Logger log = LoggerFactory.getLogger(EmprestimoService.class);

    private final EmprestimoRepository emprestimoRepository;
    private final ItemRepository itemRepository;
    private final SaidaEstoqueRepository saidaEstoqueRepository;
    private final EstoqueEventPublisher eventPublisher;

    public EmprestimoService(EmprestimoRepository emprestimoRepository,
                             ItemRepository itemRepository,
                             SaidaEstoqueRepository saidaEstoqueRepository,
                             EstoqueEventPublisher eventPublisher) {
        this.emprestimoRepository = emprestimoRepository;
        this.itemRepository = itemRepository;
        this.saidaEstoqueRepository = saidaEstoqueRepository;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public EmprestimoResponse criar(EmprestimoRequest request, EstoquePrincipal principal) {
        if (!principal.isAdmin() && !principal.idPessoa().equals(request.idPessoa())) {
            throw new ForbiddenException("Níveis não-admin só podem registrar empréstimos para si próprios");
        }
        LocalDate hoje = LocalDate.now();
        if (request.dataDevolucaoPrevista().isBefore(hoje)) {
            throw new IllegalArgumentException("Data de devolução prevista não pode ser anterior à data de hoje");
        }

        Item item = itemRepository.findByIdForUpdate(request.idItem())
                .orElseThrow(() -> new ResourceNotFoundException("Item não encontrado: " + request.idItem()));
        if (item.getQuantidadeAtual().compareTo(request.quantidade()) < 0) {
            throw new SaldoInsuficienteException(
                    "Estoque insuficiente para o item " + item.getNome() + ": saldo atual "
                            + item.getQuantidadeAtual() + ", solicitado " + request.quantidade());
        }
        item.setQuantidadeAtual(item.getQuantidadeAtual().subtract(request.quantidade()));

        Emprestimo emprestimo = new Emprestimo();
        emprestimo.setItem(item);
        emprestimo.setIdPessoa(request.idPessoa());
        emprestimo.setQuantidade(request.quantidade());
        emprestimo.setDataEmprestimo(hoje);
        emprestimo.setDataDevolucaoPrevista(request.dataDevolucaoPrevista());
        emprestimo.setStatus(StatusEmprestimo.ATIVO);
        emprestimo.setObservacao(request.observacao());
        emprestimo.setResponsavel(request.responsavel());
        emprestimo = emprestimoRepository.save(emprestimo);

        SaidaEstoque saida = new SaidaEstoque();
        saida.setItem(item);
        saida.setQuantidade(request.quantidade());
        saida.setTipoSaida(TipoSaida.EMPRESTIMO);
        saida.setIdReferencia(emprestimo.getId());
        saida.setDataSaida(LocalDateTime.now());
        saida.setObservacao("Empréstimo #" + emprestimo.getId());
        saidaEstoqueRepository.save(saida);

        verificarEstoqueBaixo(item);
        return EmprestimoMapper.toResponse(emprestimo);
    }

    @Transactional
    public EmprestimoResponse devolver(Long id, EstoquePrincipal principal) {
        Emprestimo emprestimo = emprestimoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Empréstimo não encontrado: " + id));
        if (!principal.isAdmin() && !emprestimo.getIdPessoa().equals(principal.idPessoa())) {
            throw new ForbiddenException("Apenas o Admin ou o responsável pelo empréstimo pode registrar a devolução");
        }
        if (emprestimo.getStatus() == StatusEmprestimo.DEVOLVIDO) {
            throw new IllegalArgumentException("Empréstimo já devolvido");
        }

        Item item = itemRepository.findByIdForUpdate(emprestimo.getItem().getId())
                .orElseThrow(() -> new ResourceNotFoundException("Item não encontrado: " + emprestimo.getItem().getId()));
        item.setQuantidadeAtual(item.getQuantidadeAtual().add(emprestimo.getQuantidade()));

        emprestimo.setDataDevolucaoReal(LocalDate.now());
        emprestimo.setStatus(StatusEmprestimo.DEVOLVIDO);
        return EmprestimoMapper.toResponse(emprestimo);
    }

    @Transactional(readOnly = true)
    public List<EmprestimoResponse> listarAtrasados() {
        return emprestimoRepository
                .findByStatusInAndDataDevolucaoPrevistaBefore(
                        List.of(StatusEmprestimo.ATIVO, StatusEmprestimo.ATRASADO), LocalDate.now())
                .stream()
                .map(EmprestimoMapper::toResponse)
                .toList();
    }

    /**
     * Atrasados visíveis ao principal (E-7: Admin vê todos; demais só os
     * próprios).
     */
    @Transactional(readOnly = true)
    public List<EmprestimoResponse> listarAtrasados(EstoquePrincipal principal) {
        return filtrarPorResponsavel(
                emprestimoRepository.findByStatusInAndDataDevolucaoPrevistaBefore(
                        List.of(StatusEmprestimo.ATIVO, StatusEmprestimo.ATRASADO), LocalDate.now()),
                principal).stream()
                .map(EmprestimoMapper::toResponse)
                .toList();
    }

    /**
     * Lista global de empréstimos (E-1/R-9) com escopo do principal (E-7:
     * Admin vê todos; demais só os próprios).
     *
     * @param status nulo/vazio = todos; {@code ativos} = ATIVO+ATRASADO;
     *               {@code atrasados} = vencidos; {@code historico} = DEVOLVIDO
     */
    @Transactional(readOnly = true)
    public List<EmprestimoResponse> listar(String status, EstoquePrincipal principal) {
        List<Emprestimo> emprestimos;
        if (status == null || status.isBlank()) {
            emprestimos = emprestimoRepository.findAll();
        } else if (status.equalsIgnoreCase("ativos")) {
            emprestimos = emprestimoRepository
                    .findByStatusIn(List.of(StatusEmprestimo.ATIVO, StatusEmprestimo.ATRASADO));
        } else if (status.equalsIgnoreCase("atrasados")) {
            emprestimos = emprestimoRepository
                    .findByStatusInAndDataDevolucaoPrevistaBefore(
                            List.of(StatusEmprestimo.ATIVO, StatusEmprestimo.ATRASADO), LocalDate.now());
        } else if (status.equalsIgnoreCase("historico")) {
            emprestimos = emprestimoRepository.findByStatusIn(List.of(StatusEmprestimo.DEVOLVIDO));
        } else {
            throw new IllegalArgumentException(
                    "Status inválido. Use ativos, atrasados ou historico");
        }
        return filtrarPorResponsavel(emprestimos, principal).stream()
                .map(EmprestimoMapper::toResponse).toList();
    }

    /** Busca um empréstimo pelo id (detalhe da tela {@code /emprestimos/[id]}). */
    @Transactional(readOnly = true)
    public EmprestimoResponse buscar(Long id) {
        return EmprestimoMapper.toResponse(emprestimoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Empréstimo não encontrado: " + id)));
    }

    /**
     * Detalhe com escopo do principal (E-7: Admin ou responsável pelo
     * empréstimo; demais recebem 403).
     */
    @Transactional(readOnly = true)
    public EmprestimoResponse buscar(Long id, EstoquePrincipal principal) {
        Emprestimo emprestimo = emprestimoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Empréstimo não encontrado: " + id));
        if (!principal.isAdmin() && !emprestimo.getIdPessoa().equals(principal.idPessoa())) {
            throw new ForbiddenException("Apenas o Admin ou o responsável pelo empréstimo pode consultar este empréstimo");
        }
        return EmprestimoMapper.toResponse(emprestimo);
    }

    /**
     * Marca como ATRASADO os empréstimos ativos com devolução vencida e notifica
     * via {@code emprestimo.atrasado.event}. Executado diariamente pelo
     * {@code EmprestimoAtrasadoScheduler} (03:00 por padrão).
     */
    @Transactional
    public int verificarAtrasados() {
        List<Emprestimo> atrasados = emprestimoRepository
                .findByStatusInAndDataDevolucaoPrevistaBefore(List.of(StatusEmprestimo.ATIVO), LocalDate.now());
        for (Emprestimo emprestimo : atrasados) {
            emprestimo.setStatus(StatusEmprestimo.ATRASADO);
            eventPublisher.publishEmprestimoAtrasado(emprestimo);
        }
        log.info("Marcados {} empréstimo(s) como atrasado(s)", atrasados.size());
        return atrasados.size();
    }

    private void verificarEstoqueBaixo(Item item) {
        if (item.getQuantidadeAtual().compareTo(item.getEstoqueMinimo()) <= 0) {
            eventPublisher.publishEstoqueBaixo(item);
        }
    }

    /**
     * Escopo de leitura (E-7): Admin enxerga todos; demais só os próprios
     * ({@code idPessoa} igual ao do JWT).
     */
    private List<Emprestimo> filtrarPorResponsavel(List<Emprestimo> emprestimos, EstoquePrincipal principal) {
        if (principal.isAdmin()) {
            return emprestimos;
        }
        return emprestimos.stream()
                .filter(e -> e.getIdPessoa().equals(principal.idPessoa()))
                .toList();
    }
}