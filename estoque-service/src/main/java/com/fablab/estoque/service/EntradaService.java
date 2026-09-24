package com.fablab.estoque.service;

import com.fablab.estoque.dto.EntradaRequest;
import com.fablab.estoque.dto.EntradaResponse;
import com.fablab.estoque.entity.EntradaEstoque;
import com.fablab.estoque.entity.Fornecedor;
import com.fablab.estoque.entity.Item;
import com.fablab.estoque.exception.ResourceNotFoundException;
import com.fablab.estoque.mapper.EntradaMapper;
import com.fablab.estoque.rabbit.EstoqueEventPublisher;
import com.fablab.estoque.repository.EntradaEstoqueRepository;
import com.fablab.estoque.repository.FornecedorRepository;
import com.fablab.estoque.repository.ItemRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Movimentação de entrada de estoque (compras simples).
 */
@Service
public class EntradaService {

    private final EntradaEstoqueRepository entradaEstoqueRepository;
    private final ItemRepository itemRepository;
    private final FornecedorRepository fornecedorRepository;
    private final EstoqueEventPublisher eventPublisher;

    public EntradaService(EntradaEstoqueRepository entradaEstoqueRepository,
                          ItemRepository itemRepository,
                          FornecedorRepository fornecedorRepository,
                          EstoqueEventPublisher eventPublisher) {
        this.entradaEstoqueRepository = entradaEstoqueRepository;
        this.itemRepository = itemRepository;
        this.fornecedorRepository = fornecedorRepository;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public EntradaResponse registrar(EntradaRequest request) {
        Item item = itemRepository.findByIdForUpdate(request.idItem())
                .orElseThrow(() -> new ResourceNotFoundException("Item não encontrado: " + request.idItem()));
        Fornecedor fornecedor = fornecedorRepository.findById(request.idFornecedor())
                .orElseThrow(() -> new ResourceNotFoundException("Fornecedor não encontrado: " + request.idFornecedor()));

        BigDecimal valorTotal = request.valorUnitario().multiply(request.quantidade());
        EntradaEstoque entrada = new EntradaEstoque();
        entrada.setItem(item);
        entrada.setFornecedor(fornecedor);
        entrada.setQuantidade(request.quantidade());
        entrada.setValorUnitario(request.valorUnitario());
        entrada.setValorTotal(valorTotal);
        entrada.setDataEntrada(request.dataEntrada() != null ? request.dataEntrada() : LocalDate.now());
        entrada.setNotaFiscal(request.notaFiscal());
        entrada.setObservacao(request.observacao());
        entrada.setResponsavel(request.responsavel());
        entrada = entradaEstoqueRepository.save(entrada);

        item.setQuantidadeAtual(item.getQuantidadeAtual().add(request.quantidade()));
        itemRepository.save(item);

// Compra simples é a fonte do pedido de compra padrão do fluxo.
        eventPublisher.publishCompraSolicitada(entrada);
        verificarEstoqueBaixo(item);
        return EntradaMapper.toResponse(entrada);
    }

    /** Histórico de entradas de um item. */
    @Transactional(readOnly = true)
    public List<EntradaResponse> listarPorItem(Long idItem) {
        return entradaEstoqueRepository.findByItemId(idItem).stream()
                .map(EntradaMapper::toResponse)
                .toList();
    }

    /**
     * Lista global de entradas (E-1/R-9). Sem {@code idItem} retorna todas;
     * com {@code idItem} restringe ao histórico do item (compatível).
     */
    @Transactional(readOnly = true)
    public List<EntradaResponse> listar(Long idItem) {
        if (idItem == null) {
            return entradaEstoqueRepository.findAll().stream()
                    .map(EntradaMapper::toResponse)
                    .toList();
        }
        return listarPorItem(idItem);
    }

    /** Detalhe de uma entrada (tela {@code /entradas/[id]}). */
    @Transactional(readOnly = true)
    public EntradaResponse buscar(Long id) {
        return EntradaMapper.toResponse(entradaEstoqueRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Entrada não encontrada: " + id)));
    }

    private void verificarEstoqueBaixo(Item item) {
        if (item.getQuantidadeAtual().compareTo(item.getEstoqueMinimo()) <= 0) {
            eventPublisher.publishEstoqueBaixo(item);
        }
    }
}