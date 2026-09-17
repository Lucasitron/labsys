package com.fablab.financeiro.service;

import com.fablab.financeiro.dto.CompraSolicitadaEvent;
import com.fablab.financeiro.dto.SolicitacaoCompraRequest;
import com.fablab.financeiro.dto.SolicitacaoCompraResponse;
import com.fablab.financeiro.entity.SolicitacaoCompra;
import com.fablab.financeiro.entity.StatusSolicitacaoCompra;
import com.fablab.financeiro.exception.ResourceNotFoundException;
import com.fablab.financeiro.repository.SolicitacaoCompraRepository;
import java.time.LocalDate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Solicitações de compra registradas pelo Financeiro e repassadas ao Estoque
 * como fluxo informativo ({@code compra.solicitada.event}).
 */
@Service
public class SolicitacaoCompraService {

    private final SolicitacaoCompraRepository solicitacaoRepository;
    private final FinanceiroEventPublisher eventPublisher;

    public SolicitacaoCompraService(SolicitacaoCompraRepository solicitacaoRepository,
                                    FinanceiroEventPublisher eventPublisher) {
        this.solicitacaoRepository = solicitacaoRepository;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public SolicitacaoCompraResponse criar(SolicitacaoCompraRequest request) {
        SolicitacaoCompra solicitacao = new SolicitacaoCompra();
        solicitacao.setIdItemEstoque(request.idItemEstoque());
        solicitacao.setQuantidade(request.quantidade());
        solicitacao.setValorEstimado(request.valorEstimado());
        solicitacao.setStatus(StatusSolicitacaoCompra.REGISTRADA);
        solicitacao.setDataSolicitacao(LocalDate.now());
        solicitacao = solicitacaoRepository.save(solicitacao);

        eventPublisher.publishCompraSolicitada(new CompraSolicitadaEvent(
                solicitacao.getIdSolicitacao(), null));
        return SolicitacaoCompraResponse.of(solicitacao);
    }

    @Transactional
    public SolicitacaoCompraResponse concluir(Long idSolicitacao) {
        SolicitacaoCompra solicitacao = solicitacaoRepository.findById(idSolicitacao)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Solicitação de compra não encontrada: " + idSolicitacao));
        if (solicitacao.getStatus() == StatusSolicitacaoCompra.CONCLUIDA) {
            throw new IllegalArgumentException("Solicitação de compra já concluída");
        }
        solicitacao.setStatus(StatusSolicitacaoCompra.CONCLUIDA);
        return SolicitacaoCompraResponse.of(solicitacaoRepository.save(solicitacao));
    }
}