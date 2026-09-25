package com.fablab.estoque.service;

import com.fablab.estoque.dto.SaidaRequest;
import com.fablab.estoque.dto.SaidaResponse;
import com.fablab.estoque.entity.Item;
import com.fablab.estoque.entity.SaidaEstoque;
import com.fablab.estoque.exception.ResourceNotFoundException;
import com.fablab.estoque.exception.SaldoInsuficienteException;
import com.fablab.estoque.mapper.SaidaMapper;
import com.fablab.estoque.repository.ItemRepository;
import com.fablab.estoque.repository.SaidaEstoqueRepository;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Movimentação de saída manual de estoque (consumo, perda, ajuste).
 */
@Service
public class SaidaService {

    private final SaidaEstoqueRepository saidaEstoqueRepository;
    private final ItemRepository itemRepository;
    private final ItemService itemService;

    public SaidaService(SaidaEstoqueRepository saidaEstoqueRepository,
                        ItemRepository itemRepository,
                        ItemService itemService) {
        this.saidaEstoqueRepository = saidaEstoqueRepository;
        this.itemRepository = itemRepository;
        this.itemService = itemService;
    }

    @Transactional
    public SaidaResponse registrar(SaidaRequest request) {
        Item item = itemRepository.findByIdForUpdate(request.idItem())
                .orElseThrow(() -> new ResourceNotFoundException("Item não encontrado: " + request.idItem()));

        BigDecimal novoSaldo = item.getQuantidadeAtual().subtract(request.quantidade());
        if (novoSaldo.signum() < 0) {
            throw new SaldoInsuficienteException(
                    "Estoque insuficiente para o item " + item.getNome() + ": saldo atual "
                            + item.getQuantidadeAtual() + ", solicitado " + request.quantidade());
        }
        item.setQuantidadeAtual(novoSaldo);

        SaidaEstoque saida = new SaidaEstoque();
        saida.setItem(item);
        saida.setQuantidade(request.quantidade());
        saida.setTipoSaida(request.tipoSaida());
        saida.setIdReferencia(request.idReferencia());
        saida.setDataSaida(LocalDateTime.now());
        saida.setObservacao(request.observacao());
        saida.setResponsavel(request.responsavel());
        saida = saidaEstoqueRepository.save(saida);

        itemService.verificarEstoqueBaixo(item);
        return SaidaMapper.toResponse(saida);
    }

    /** Histórico de saídas de um item. */
    @Transactional(readOnly = true)
    public List<SaidaResponse> listarPorItem(Long idItem) {
        return saidaEstoqueRepository.findByItemId(idItem).stream()
                .map(SaidaMapper::toResponse)
                .toList();
    }

    /**
     * Lista global de saídas (E-1/R-9). Sem {@code idItem} retorna todas;
     * com {@code idItem} restringe ao histórico do item (compatível).
     */
    @Transactional(readOnly = true)
    public List<SaidaResponse> listar(Long idItem) {
        if (idItem == null) {
            return saidaEstoqueRepository.findAll().stream()
                    .map(SaidaMapper::toResponse)
                    .toList();
        }
        return listarPorItem(idItem);
    }

    /** Detalhe de uma saída (tela {@code /saidas/[id]}). */
    @Transactional(readOnly = true)
    public SaidaResponse buscar(Long id) {
        return SaidaMapper.toResponse(saidaEstoqueRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Saída não encontrada: " + id)));
    }
}