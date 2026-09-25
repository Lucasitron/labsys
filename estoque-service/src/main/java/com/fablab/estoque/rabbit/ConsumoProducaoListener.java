package com.fablab.estoque.rabbit;

import com.fablab.estoque.config.RabbitConfig;
import com.fablab.estoque.dto.ProducaoConcluidaEvent;
import com.fablab.estoque.service.ItemService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Consome {@code producao.concluida.event} do Produção &amp; Projetos Service e
 * efetua a baixa automática de estoque pelos itens realmente consumidos.
 *
 * <p>A baixa é idempotente por {@code id_referencia} (id da encomenda).</p>
 */
@Component
public class ConsumoProducaoListener {

    private static final Logger log = LoggerFactory.getLogger(ConsumoProducaoListener.class);

    private final ItemService itemService;

    public ConsumoProducaoListener(ItemService itemService) {
        this.itemService = itemService;
    }

    @RabbitListener(queues = RabbitConfig.CONSUMO_BOM_QUEUE)
    @Transactional
    public void onProducaoConcluida(ProducaoConcluidaEvent event) {
        log.info("Recebido producao.concluida.event para a encomenda {}", event.idEncomenda());
        if (event.itens() == null || event.itens().isEmpty()) {
            return;
        }
        itemService.baixarPorConsumo(event.itens(), event.idEncomenda());
    }
}