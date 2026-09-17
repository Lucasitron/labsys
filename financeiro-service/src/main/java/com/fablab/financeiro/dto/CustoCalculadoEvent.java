package com.fablab.financeiro.dto;

import java.math.BigDecimal;

/**
 * Evento {@code custo.calculado.event} publicado para o Vendas &amp; CRM Service
 * quando o custo de uma encomenda é calculado (fluxo informativo).
 *
 * @param idEncomenda id da encomenda
 * @param custoTotal custo total apurado (materiais + mão de obra + overhead)
 * @param margemLucro margem de lucro (valor de venda - custo total)
 */
public record CustoCalculadoEvent(
        Long idEncomenda,
        BigDecimal custoTotal,
        BigDecimal margemLucro) {
}