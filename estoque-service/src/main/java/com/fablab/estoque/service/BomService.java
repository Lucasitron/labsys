package com.fablab.estoque.service;

import com.fablab.estoque.dto.BomConsumoItem;
import com.fablab.estoque.dto.BomConsumoRequest;
import com.fablab.estoque.dto.BomItemRequest;
import com.fablab.estoque.dto.BomRequest;
import com.fablab.estoque.dto.BomResponse;
import com.fablab.estoque.entity.Item;
import com.fablab.estoque.entity.ItemBom;
import com.fablab.estoque.entity.ListaMateriais;
import com.fablab.estoque.exception.ResourceNotFoundException;
import com.fablab.estoque.mapper.BomMapper;
import com.fablab.estoque.repository.ItemRepository;
import com.fablab.estoque.repository.ListaMateriaisRepository;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Gestão de Listas de Materiais (BOM) e registro de consumo real.
 */
@Service
public class BomService {

    private final ListaMateriaisRepository bomRepository;
    private final ItemRepository itemRepository;
    private final ItemService itemService;

    public BomService(ListaMateriaisRepository bomRepository,
                      ItemRepository itemRepository,
                      ItemService itemService) {
        this.bomRepository = bomRepository;
        this.itemRepository = itemRepository;
        this.itemService = itemService;
    }

    @Transactional
    public BomResponse criar(BomRequest request) {
        ListaMateriais bom = new ListaMateriais();
        bom.setIdProdutoServico(request.idProdutoServico());
        bom.setNome(request.nome());
        bom.setVersao(request.versao() != null ? request.versao() : 1);
        bom.setEditavel(request.editavel() != null ? request.editavel() : true);
        preencherItens(bom, request);
        return BomMapper.toResponse(bomRepository.save(bom));
    }

    @Transactional(readOnly = true)
    public BomResponse buscar(Long id) {
        return BomMapper.toResponse(obter(id));
    }

    @Transactional
    public BomResponse atualizar(Long id, BomRequest request) {
        ListaMateriais bom = obter(id);
        bom.setNome(request.nome());
        bom.setVersao(request.versao() != null ? request.versao() : bom.getVersao() + 1);
        bom.setEditavel(request.editavel() != null ? request.editavel() : bom.getEditavel());
        List<ItemBom> novos = new ArrayList<>();
        for (BomItemRequest itemRequest : request.itens()) {
            ItemBom itemBom = bom.getItens().stream()
                    .filter(ib -> ib.getItem().getId().equals(itemRequest.idItem()))
                    .findFirst()
                    .orElseGet(() -> {
                        ItemBom novo = new ItemBom();
                        novo.setItem(itemRepository.findById(itemRequest.idItem())
                                .orElseThrow(() -> new ResourceNotFoundException(
                                        "Item não encontrado: " + itemRequest.idItem())));
                        novo.setBom(bom);
                        novos.add(novo);
                        return novo;
                    });
            itemBom.setQuantidadePrevista(itemRequest.quantidadePrevista());
        }
        bom.getItens().removeIf(ib -> request.itens().stream()
                .noneMatch(r -> r.idItem().equals(ib.getItem().getId())));
        bom.getItens().addAll(novos);
        return BomMapper.toResponse(bomRepository.save(bom));
    }

    /**
     * Registra o consumo real dos itens da BOM durante a produção e efetua a
     * baixa no estoque (saída CPNSUMO com {@code id_referencia} = id da BOM).
     */
    @Transactional
    public BomResponse registrarConsumo(Long id, BomConsumoRequest request) {
        ListaMateriais bom = obter(id);
        for (BomConsumoItem consumo : request.itens()) {
            ItemBom itemBom = bom.getItens().stream()
                    .filter(ib -> ib.getItem().getId().equals(consumo.idItem()))
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException(
                            "Item " + consumo.idItem() + " não pertence à BOM " + id));
            itemBom.setQuantidadeReal(consumo.quantidadeConsumida());
            itemService.registrarConsumo(consumo.idItem(), consumo.quantidadeConsumida(), id);
        }
        return BomMapper.toResponse(bom);
    }

    private ListaMateriais obter(Long id) {
        return bomRepository.findWithItensById(id)
                .orElseThrow(() -> new ResourceNotFoundException("BOM não encontrada: " + id));
    }

    private void preencherItens(ListaMateriais bom, BomRequest request) {
        for (BomItemRequest itemRequest : request.itens()) {
            Item item = itemRepository.findById(itemRequest.idItem())
                    .orElseThrow(() -> new ResourceNotFoundException("Item não encontrado: " + itemRequest.idItem()));
            ItemBom itemBom = new ItemBom();
            itemBom.setItem(item);
            itemBom.setQuantidadePrevista(itemRequest.quantidadePrevista());
            bom.adicionarItem(itemBom);
        }
    }
}