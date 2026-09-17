package com.fablab.financeiro.consumer;

import com.fablab.financeiro.config.RabbitMqConfig;
import com.fablab.financeiro.dto.ProducaoConcluidaEvent;
import com.fablab.financeiro.service.CustoService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * Consome {@code producao.concluida.event} do Produção &amp; Projetos e calcula
 * o custo da encomenda concluída (Job Order Costing).
 */
@Component
public class ProducaoConcluidaListener {

    private static final Logger log = LoggerFactory.getLogger(ProducaoConcluidaListener.class);

    private final CustoService custoService;

    public ProducaoConcluidaListener(CustoService custoService) {
        this.custoService = custoService;
    }

    @RabbitListener(queues = RabbitMqConfig.PRODUCAO_CONCLUIDA_QUEUE)
    public void aoConcluirProducao(ProducaoConcluidaEvent event) {
        try {
            custoService.calcularCusto(event.idEncomenda());
            log.info("Custo calculado para a encomenda {}", event.idEncomenda());
        } catch (RuntimeException ex) {
            log.warn("Falha ao calcular custo da encomenda {}: {}",
                    event.idEncomenda(), ex.getMessage());
        }
    }
}