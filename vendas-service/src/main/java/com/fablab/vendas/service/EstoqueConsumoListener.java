package com.fablab.vendas.service;

import com.fablab.vendas.config.RabbitMqConfig;
import com.fablab.vendas.dto.EstoqueConsumoEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * Consome {@code estoque.consumo.realizado.event} do Estoque Service para
 * vincular o consumo real de materiais ao custo da encomenda (fluxo
 * informativo para estimativa de lucro).
 */
@Component
public class EstoqueConsumoListener {

    private static final Logger log = LoggerFactory.getLogger(EstoqueConsumoListener.class);

    @RabbitListener(queues = RabbitMqConfig.ESTOQUE_CONSUMO_QUEUE)
    public void onConsumoRealizado(EstoqueConsumoEvent event) {
        log.info("Consumo real recebido para encomenda {}: item {} qtd {}",
                event.idEncomenda(), event.idItem(), event.quantidadeConsumida());
    }
}