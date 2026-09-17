package com.fablab.financeiro.dto;

import java.math.BigDecimal;
import java.util.List;

/**
 * Evento {@code producao.concluida.event} consumido do Produção &amp; Projetos
 * Service. Dispara o cálculo do custo da encomenda (Job Order Costing).
 *
 * @param idEncomenda id da encomenda
 * @param idProdutoServico referência do produto/serviço produzido
 * @param itens itens consumidos (somente informação complementar)
 */
public record ProducaoConcluidaEvent(
        Long idEncomenda,
        Long idProdutoServico,
        List<ItemConsumido> itens) {

    /** Item consumido na produção (pagamento informativo). */
    public record ItemConsumido(
            Long idItem,
            BigDecimal quantidade) {
    }
}