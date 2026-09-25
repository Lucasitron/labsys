package com.fablab.producao.dto;

import com.fablab.producao.entity.ItemInspecao5S;

/** Representação de um item de inspeção 5S. */
public record ItemInspecaoResponse(
        Long idItemInspecao,
        Long idChecklist,
        String item,
        Boolean conforme,
        String observacao) {

    public static ItemInspecaoResponse from(ItemInspecao5S item) {
        return new ItemInspecaoResponse(
                item.getIdItemInspecao(),
                item.getChecklist().getIdChecklist(),
                item.getChecklist().getItem(),
                item.getConforme(),
                item.getObservacao());
    }
}