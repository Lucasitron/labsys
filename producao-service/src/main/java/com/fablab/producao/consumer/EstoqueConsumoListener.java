package com.fablab.producao.consumer;

import com.fablab.producao.config.RabbitMqConfig;
import com.fablab.producao.dto.EstoqueConsumoEvent;
import com.fablab.producao.entity.ConsumoEncomenda;
import com.fablab.producao.repository.ConsumoEncomendaRepository;
import java.math.BigDecimal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Consome {@code estoque.consumo.realizado.event} e acumula o consumo real de
 * itens por encomenda. Esse acumulado é reemitido em {@code producao.concluida.event}
 * para a baixa de estoque e o custeio.
 */
@Component
public class EstoqueConsumoListener {

    private static final Logger log = LoggerFactory.getLogger(EstoqueConsumoListener.class);

    private final ConsumoEncomendaRepository consumoRepository;

    public EstoqueConsumoListener(ConsumoEncomendaRepository consumoRepository) {
        this.consumoRepository = consumoRepository;
    }

    @RabbitListener(queues = RabbitMqConfig.ESTOQUE_CONSUMO_QUEUE)
    @Transactional
    public void onConsumoRealizado(EstoqueConsumoEvent event) {
        if (event.idEncomenda() == null || event.idItem() == null || event.quantidadeConsumida() == null) {
            log.warn("Evento estoque.consumo.realizado incompleto; ignorando: {}", event);
            return;
        }
        ConsumoEncomenda consumo = consumoRepository.findByIdEncomenda(event.idEncomenda()).stream()
                .filter(c -> c.getIdItem().equals(event.idItem()))
                .findFirst()
                .orElseGet(() -> {
                    ConsumoEncomenda novo = new ConsumoEncomenda();
                    novo.setIdEncomenda(event.idEncomenda());
                    novo.setIdItem(event.idItem());
                    novo.setQuantidadeConsumida(BigDecimal.ZERO);
                    return novo;
                });
        consumo.setQuantidadeConsumida(consumo.getQuantidadeConsumida().add(event.quantidadeConsumida()));
        consumoRepository.save(consumo);
        log.info("Consumo da encomenda {} item {} acumulado em {}",
                event.idEncomenda(), event.idItem(), consumo.getQuantidadeConsumida());
    }
}