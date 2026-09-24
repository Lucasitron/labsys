package com.fablab.financeiro.service;

import com.fablab.financeiro.dto.FinanceiroEventos.LancamentoVencidoEvent;
import com.fablab.financeiro.dto.LancamentoDtos.LancamentoListaResponse;
import com.fablab.financeiro.dto.LancamentoDtos.LancamentoRequest;
import com.fablab.financeiro.dto.LancamentoDtos.LancamentoResponse;
import com.fablab.financeiro.dto.LancamentoDtos.PagamentoRequest;
import com.fablab.financeiro.dto.LancamentoDtos.ResumoResponse;
import com.fablab.financeiro.entity.LancamentoFinanceiro;
import com.fablab.financeiro.entity.StatusLancamento;
import com.fablab.financeiro.entity.TipoLancamento;
import com.fablab.financeiro.exception.ResourceNotFoundException;
import com.fablab.financeiro.repository.CategoriaFinanceiraRepository;
import com.fablab.financeiro.repository.LancamentoFinanceiroRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Contas a pagar/receber: criação, listagem com filtros, liquidação e varredura de vencidos. */
@Service
public class LancamentoFinanceiroService {

    private final LancamentoFinanceiroRepository repository;
    private final CategoriaFinanceiraRepository categoriaRepository;
    private final FinanceiroEventPublisher publisher;
    private final PaymentProcessor paymentProcessor;

    public LancamentoFinanceiroService(LancamentoFinanceiroRepository repository,
                                       CategoriaFinanceiraRepository categoriaRepository,
                                       FinanceiroEventPublisher publisher,
                                       PaymentProcessor paymentProcessor) {
        this.repository = repository;
        this.categoriaRepository = categoriaRepository;
        this.publisher = publisher;
        this.paymentProcessor = paymentProcessor;
    }

    @Transactional
    public LancamentoResponse criar(LancamentoRequest request) {
        categoriaRepository.findById(request.idCategoria())
                .orElseThrow(() -> new ResourceNotFoundException("Categoria não encontrada"));
        LancamentoFinanceiro entity = new LancamentoFinanceiro();
        entity.setIdCategoria(request.idCategoria());
        entity.setTipo(request.tipo());
        entity.setValor(request.valor());
        entity.setDataVencimento(request.dataVencimento());
        entity.setIdReferenciaExterna(request.idReferenciaExterna());
        entity.setObservacao(request.observacao());
        if (request.dataVencimento().isBefore(LocalDate.now())) {
            entity.setStatus(StatusLancamento.ATRASADO);
        } else {
            entity.setStatus(StatusLancamento.PENDENTE);
        }
        LancamentoFinanceiro salvo = repository.save(entity);
        if (salvo.getStatus() == StatusLancamento.ATRASADO) {
            publisher.publishLancamentoVencido(new LancamentoVencidoEvent(
                    salvo.getId(), salvo.getValor(), salvo.getDataVencimento(),
                    salvo.getIdReferenciaExterna()));
        }
        return LancamentoResponse.of(salvo);
    }

    /**
     * Lista com filtros combinados (D-4). {@code origem} filtra por trecho da
     * referência externa; {@code vencimentoDe}/{@code vencimentoAte} delimitam o
     * vencimento (D-8, semântica assumida — front aplica por página).
     */
    @Transactional(readOnly = true)
    public LancamentoListaResponse listar(StatusLancamento status, TipoLancamento tipo,
                                          Long idCategoria, LocalDate dataInicio, LocalDate dataFim,
                                          String origem, LocalDate vencimentoDe, LocalDate vencimentoAte) {
        LocalDate inicio = dataInicio != null ? dataInicio : vencimentoDe;
        LocalDate fim = dataFim != null ? dataFim : vencimentoAte;
        List<LancamentoFinanceiro> todos = repository.findAll();
        List<LancamentoFinanceiro> filtrados = todos.stream()
                .filter(l -> status == null || l.getStatus() == status)
                .filter(l -> tipo == null || l.getTipo() == tipo)
                .filter(l -> idCategoria == null || idCategoria.equals(l.getIdCategoria()))
                .filter(l -> inicio == null || !l.getDataVencimento().isBefore(inicio))
                .filter(l -> fim == null || !l.getDataVencimento().isAfter(fim))
                .filter(l -> origem == null || origem.isBlank()
                        || (l.getIdReferenciaExterna() != null && l.getIdReferenciaExterna().contains(origem)))
                .toList();

        Map<String, Long> counts = filtrados.stream()
                .collect(Collectors.groupingBy(l -> l.getStatus().name(), Collectors.counting()));
        for (StatusLancamento s : StatusLancamento.values()) {
            counts.putIfAbsent(s.name(), 0L);
        }

        BigDecimal entradas = filtrados.stream()
                .filter(l -> l.getTipo() == TipoLancamento.ENTRADA && l.getStatus() != StatusLancamento.CANCELADO)
                .map(LancamentoFinanceiro::getValor).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal saidas = filtrados.stream()
                .filter(l -> l.getTipo() == TipoLancamento.SAIDA && l.getStatus() != StatusLancamento.CANCELADO)
                .map(LancamentoFinanceiro::getValor).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal pendente = filtrados.stream()
                .filter(l -> (l.getStatus() == StatusLancamento.PENDENTE || l.getStatus() == StatusLancamento.ATRASADO))
                .map(LancamentoFinanceiro::getValor).reduce(BigDecimal.ZERO, BigDecimal::add);

        List<LancamentoResponse> itens = filtrados.stream().map(LancamentoResponse::of).toList();
        return new LancamentoListaResponse(itens, counts,
                new ResumoResponse(entradas, saidas, pendente, entradas.subtract(saidas)));
    }

    @Transactional
    public LancamentoResponse registrarPagamento(Long id, PagamentoRequest request) {
        LancamentoFinanceiro entity = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Lançamento não encontrado"));
        if (entity.getStatus() == StatusLancamento.PAGO || entity.getStatus() == StatusLancamento.CANCELADO) {
            throw new IllegalArgumentException("Lançamento já liquidado ou cancelado não pode ser pago");
        }
        entity.setStatus(StatusLancamento.PAGO);
        entity.setDataPagamento(request != null && request.dataPagamento() != null
                ? request.dataPagamento() : LocalDate.now());
        if (request != null && request.observacao() != null) {
            entity.setObservacao(request.observacao());
        }
        paymentProcessor.registrarPagamentoManual(entity.getId(), entity.getObservacao());
        return LancamentoResponse.of(repository.save(entity));
    }

    /**
     * Varredura idempotente de vencidos: publica
     * {@code lancamento.vencido.event} por lançamento PENDENTE/ATRASADO com
     * vencimento anterior a hoje. Não liquida nada (apenas notifica).
     *
     * @return número de eventos publicados
     */
    @Transactional
    public int emitirVencidos() {
        List<LancamentoFinanceiro> vencidos = repository.findByStatusInAndDataVencimentoBefore(
                List.of(StatusLancamento.PENDENTE, StatusLancamento.ATRASADO), LocalDate.now());
        for (LancamentoFinanceiro lancamento : vencidos) {
            if (lancamento.getStatus() == StatusLancamento.PENDENTE) {
                lancamento.setStatus(StatusLancamento.ATRASADO);
                repository.save(lancamento);
            }
            publisher.publishLancamentoVencido(new LancamentoVencidoEvent(
                    lancamento.getId(), lancamento.getValor(), lancamento.getDataVencimento(),
                    lancamento.getIdReferenciaExterna()));
        }
        return vencidos.size();
    }
}
