package com.fablab.financeiro.service;

import com.fablab.financeiro.dto.LancamentoFinanceiroRequest;
import com.fablab.financeiro.dto.LancamentoFinanceiroResponse;
import com.fablab.financeiro.dto.LancamentoVencidoEvent;
import com.fablab.financeiro.entity.CategoriaFinanceira;
import com.fablab.financeiro.entity.LancamentoFinanceiro;
import com.fablab.financeiro.entity.StatusLancamento;
import com.fablab.financeiro.exception.ResourceNotFoundException;
import com.fablab.financeiro.repository.CategoriaFinanceiraRepository;
import com.fablab.financeiro.repository.LancamentoFinanceiroRepository;
import java.time.LocalDate;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Lançamentos financeiros (contas a pagar/receber) e notificação de vencidos.
 */
@Service
public class LancamentoFinanceiroService {

    private final LancamentoFinanceiroRepository lancamentoRepository;
    private final CategoriaFinanceiraRepository categoriaRepository;
    private final FinanceiroEventPublisher eventPublisher;

    public LancamentoFinanceiroService(LancamentoFinanceiroRepository lancamentoRepository,
                                       CategoriaFinanceiraRepository categoriaRepository,
                                       FinanceiroEventPublisher eventPublisher) {
        this.lancamentoRepository = lancamentoRepository;
        this.categoriaRepository = categoriaRepository;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public LancamentoFinanceiroResponse criar(LancamentoFinanceiroRequest request) {
        CategoriaFinanceira categoria = categoriaRepository.findById(request.idCategoria())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Categoria não encontrada: " + request.idCategoria()));

        LancamentoFinanceiro lancamento = new LancamentoFinanceiro();
        lancamento.setCategoria(categoria);
        lancamento.setTipo(request.tipo());
        lancamento.setValor(request.valor());
        lancamento.setDataVencimento(request.dataVencimento());
        lancamento.setStatus(LocalDate.now().isAfter(request.dataVencimento())
                ? StatusLancamento.ATRASADO : StatusLancamento.PENDENTE);
        lancamento.setIdReferenciaExterna(request.idReferenciaExterna());
        lancamento.setObservacao(request.observacao());

        lancamento = lancamentoRepository.save(lancamento);

        if (lancamento.getStatus() == StatusLancamento.ATRASADO) {
            eventPublisher.publishLancamentoVencido(new LancamentoVencidoEvent(
                    lancamento.getIdLancamento(), lancamento.getTipo(),
                    lancamento.getValor(), lancamento.getDataVencimento()));
        }

        return LancamentoFinanceiroResponse.of(lancamento);
    }

    @Transactional(readOnly = true)
    public List<LancamentoFinanceiroResponse> listar(StatusLancamento status, LocalDate dataInicio,
                                                     LocalDate dataFim, Long idCategoria) {
        List<LancamentoFinanceiro> lancamentos;
        if (status != null) {
            lancamentos = lancamentoRepository.findByStatus(status);
        } else if (dataFim != null) {
            LocalDate inicio = dataInicio != null ? dataInicio : LocalDate.EPOCH;
            lancamentos = lancamentoRepository.findByDataVencimentoBetween(inicio, dataFim);
        } else if (idCategoria != null) {
            lancamentos = lancamentoRepository.findByCategoria_IdCategoria(idCategoria);
        } else {
            lancamentos = lancamentoRepository.findAll();
        }
        return lancamentos.stream().map(LancamentoFinanceiroResponse::of).toList();
    }

    @Transactional
    public LancamentoFinanceiroResponse registrarPagamento(Long idLancamento) {
        LancamentoFinanceiro lancamento = obter(idLancamento);
        if (lancamento.getStatus() == StatusLancamento.PAGO
                || lancamento.getStatus() == StatusLancamento.CANCELADO) {
            throw new IllegalArgumentException(
                    "Lançamento " + lancamento.getStatus().name().toLowerCase() + " não pode ser liquidado");
        }
        lancamento.setStatus(StatusLancamento.PAGO);
        lancamento.setDataPagamento(LocalDate.now());
        return LancamentoFinanceiroResponse.of(lancamentoRepository.save(lancamento));
    }

    /**
     * Varre os lançamentos vencidos não liquidados e publica
     * {@code lancamento.vencido.event} para cada um.
     *
     * @return quantidade de eventos publicados
     */
    @Transactional(readOnly = true)
    public long emitirVencidos() {
        List<LancamentoFinanceiro> vencidos = lancamentoRepository
                .findByStatusInAndDataVencimentoBefore(
                        List.of(StatusLancamento.PENDENTE, StatusLancamento.ATRASADO),
                        LocalDate.now());
        vencidos.forEach(l -> eventPublisher.publishLancamentoVencido(new LancamentoVencidoEvent(
                l.getIdLancamento(), l.getTipo(), l.getValor(), l.getDataVencimento())));
        return vencidos.size();
    }

    private LancamentoFinanceiro obter(Long id) {
        return lancamentoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Lançamento não encontrado: " + id));
    }
}