package com.fablab.estoque.dto;

import java.util.List;

/**
 * Evento {@code producao.concluida.event} do Produção &amp; Projetos Service.
 * Dispara a baixa automática de estoque conforme o consumo real.
 */
public record ProducaoConcluidaEvent(
        Long idEncomenda,
        Long idProdutoServico,
        List<ItemConsumido> itens) {
}