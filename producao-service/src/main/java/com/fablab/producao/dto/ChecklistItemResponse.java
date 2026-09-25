package com.fablab.producao.dto;

import com.fablab.producao.entity.SetorChecklist;

/** Representação de um item de checklist. */
public record ChecklistItemResponse(Long idChecklist, String item, Boolean ativo) {

    public static ChecklistItemResponse from(SetorChecklist checklist) {
        return new ChecklistItemResponse(checklist.getIdChecklist(), checklist.getItem(), checklist.getAtivo());
    }
}