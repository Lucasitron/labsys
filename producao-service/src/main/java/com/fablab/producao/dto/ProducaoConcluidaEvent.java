package com.fablab.producao.dto;

import java.time.LocalDate;
import java.util.List;

/**
 * Evento {@code producao.concluida.event} publicado pelo Produção quando a
 * encomenda é finalizada.
 *
 * <p>Consumido pelo Estoque (baixa de itens reais) e pelo Financeiro (custeio da
 * encomenda). O campo {@code dataConclusao} atende também ao contrato descrito
 * na especificação do módulo.</p>
 */
public record ProducaoConcluidaEvent(
        Long idEncomenda,
        Long idProdutoServico,
        List<ItemConsumido> itens,
        LocalDate dataConclusao) {
}