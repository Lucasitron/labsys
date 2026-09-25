package com.fablab.estoque.mapper;

import com.fablab.estoque.dto.ItemRequest;
import com.fablab.estoque.dto.ItemResponse;
import com.fablab.estoque.dto.LocalizacaoResponse;
import com.fablab.estoque.entity.Item;
import com.fablab.estoque.entity.Localizacao;

/**
 * Mapeia entidades {@code Item} para DTOs e aplica atualizações.
 */
public final class ItemMapper {

    private ItemMapper() {
    }

    public static Item toEntity(ItemRequest request, Localizacao localizacao) {
        Item item = new Item();
        item.setNome(request.nome());
        item.setDescricao(request.descricao());
        item.setCategoria(request.categoria());
        item.setUnidadeMedida(request.unidadeMedida());
        item.setQuantidadeAtual(request.quantidadeAtual());
        item.setEstoqueMinimo(request.estoqueMinimo());
        item.setLocalizacao(localizacao);
        return item;
    }

    public static void update(Item item, ItemRequest request, Localizacao localizacao) {
        item.setNome(request.nome());
        item.setDescricao(request.descricao());
        item.setCategoria(request.categoria());
        item.setUnidadeMedida(request.unidadeMedida());
        item.setEstoqueMinimo(request.estoqueMinimo());
        item.setLocalizacao(localizacao);
    }

    public static ItemResponse toResponse(Item item) {
        LocalizacaoResponse localizacao = LocalizacaoMapper.toResponse(item.getLocalizacao());
        return new ItemResponse(
                item.getId(),
                item.getNome(),
                item.getDescricao(),
                item.getCategoria(),
                item.getUnidadeMedida(),
                item.getQuantidadeAtual(),
                item.getEstoqueMinimo(),
                localizacao);
    }
}