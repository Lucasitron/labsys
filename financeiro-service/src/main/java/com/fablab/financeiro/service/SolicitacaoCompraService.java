package com.fablab.financeiro.service;

import com.fablab.financeiro.dto.CompraDtos.CompraRequest;
import com.fablab.financeiro.dto.CompraDtos.CompraResponse;
import com.fablab.financeiro.dto.FinanceiroEventos.CompraSolicitadaEvent;
import com.fablab.financeiro.entity.SolicitacaoCompra;
import com.fablab.financeiro.entity.StatusCompra;
import com.fablab.financeiro.exception.ResourceNotFoundException;
import com.fablab.financeiro.repository.SolicitacaoCompraRepository;
import java.time.LocalDate;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Fluxo informativo de compras: registra com {@code REGISTRADA}, publica
 * {@code compra.solicitada.event} e conclui para {@code CONCLUIDA}. Não há
 * bloqueio da operação.
 */
@Service
public class SolicitacaoCompraService {

    private final SolicitacaoCompraRepository repository;
    private final FinanceiroEventPublisher publisher;

    public SolicitacaoCompraService(SolicitacaoCompraRepository repository,
                                    FinanceiroEventPublisher publisher) {
        this.repository = repository;
        this.publisher = publisher;
    }

    @Transactional
    public CompraResponse registrar(CompraRequest request) {
        SolicitacaoCompra entity = new SolicitacaoCompra();
        entity.setIdItemEstoque(request.idItemEstoque());
        entity.setQuantidade(request.quantidade());
        entity.setValorEstimado(request.valorEstimado());
        entity.setStatus(StatusCompra.REGISTRADA);
        entity.setDataSolicitacao(LocalDate.now());
        SolicitacaoCompra salva = repository.save(entity);
        publisher.publishCompraSolicitada(new CompraSolicitadaEvent(salva.getId(), null));
        return CompraResponse.of(salva);
    }

    @Transactional(readOnly = true)
    public List<CompraResponse> listar(StatusCompra status) {
        List<SolicitacaoCompra> entities = status == null ? repository.findAll() : repository.findByStatus(status);
        return entities.stream().map(CompraResponse::of).toList();
    }

    @Transactional(readOnly = true)
    public CompraResponse detalhar(Long id) {
        return CompraResponse.of(buscar(id));
    }

    @Transactional
    public CompraResponse marcarVisualizada(Long id) {
        SolicitacaoCompra entity = buscar(id);
        if (entity.getStatus() == StatusCompra.REGISTRADA) {
            entity.setStatus(StatusCompra.VISUALIZADA);
        }
        return CompraResponse.of(repository.save(entity));
    }

    @Transactional
    public CompraResponse concluir(Long id) {
        SolicitacaoCompra entity = buscar(id);
        if (entity.getStatus() == StatusCompra.CONCLUIDA) {
            throw new IllegalArgumentException("Solicitação de compra já concluída");
        }
        entity.setStatus(StatusCompra.CONCLUIDA);
        return CompraResponse.of(repository.save(entity));
    }

    private SolicitacaoCompra buscar(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Solicitação de compra não encontrada"));
    }
}
