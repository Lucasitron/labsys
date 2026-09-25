package com.fablab.producao.consumer;

import com.fablab.producao.config.RabbitMqConfig;
import com.fablab.producao.dto.NivelAlteradoEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * Consome {@code nivel.alterado.event} do Pessoas &amp; RH para manter o
 * Produção ciente das mudanças de permissão dos membros. O nível efetivo já é
 * aplicado nas requisições via JWT; este listener registra a alteração para
 * trilha de auditoria.
 */
@Component
public class NivelAlteradoListener {

    private static final Logger log = LoggerFactory.getLogger(NivelAlteradoListener.class);

    @RabbitListener(queues = RabbitMqConfig.NIVEL_ALTERADO_QUEUE)
    public void onNivelAlterado(NivelAlteradoEvent event) {
        log.info("Nível de acesso do funcionário {} alterado de {} para {} em {}",
                event.idFuncionario(), event.nivelAntigo(), event.nivelNovo(), event.data());
    }
}