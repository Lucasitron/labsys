package com.fablab.estoque.mapper;

import com.fablab.estoque.dto.BomItemResponse;
import com.fablab.estoque.dto.BomResponse;
import com.fablab.estoque.entity.ItemBom;
import com.fablab.estoque.entity.ListaMateriais;
import java.util.Comparator;
import java.util.List;

/**
 * Mapeia entidades {@code ListaMateriais} para DTOs.
 */
public final class BomMapper {

    private BomMapper() {
    }

    public static BomResponse toResponse(ListaMateriais bom) {
        List<BomItemResponse> itens = bom.getItens().stream()
                .sorted(Comparator.comparing(ItemBom::getId))
                .map(BomMapper::toItemResponse)
                .toList();
        return new BomResponse(
                bom.getId(),
                bom.getIdProdutoServico(),
                bom.getNome(),
                bom.getVersao(),
                bom.getEditavel(),
                itens);
    }

    private static BomItemResponse toItemResponse(ItemBom itemBom) {
        return new BomItemResponse(
                itemBom.getItem().getId(),
                itemBom.getItem().getNome(),
                itemBom.getQuantidadePrevista(),
                itemBom.getQuantidadeReal());
    }
}