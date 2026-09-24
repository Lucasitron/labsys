package com.fablab.dashboard.dto;

/**
 * Item da timeline de atividade recente.
 *
 * @param id     identificador do evento
 * @param actor  autor (ou {@code Você})
 * @param verb   verbo da ação servido pronto
 * @param target alvo da ação
 * @param module módulo do evento
 * @param at     instante do evento (ISO)
 */
public record ActivityItemDto(String id, String actor, String verb, String target, String module, String at) {
}