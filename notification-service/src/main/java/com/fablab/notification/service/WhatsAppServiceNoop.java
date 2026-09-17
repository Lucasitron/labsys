package com.fablab.notification.service;

import com.fablab.notification.entity.Notificacao;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Implementação no-op do {@link WhatsAppService}: apenas registra em log, sem
 * enviar nada. Mantém a estrutura pronta para uma futura integração.
 */
@Service
public class WhatsAppServiceNoop implements WhatsAppService {

    private static final Logger log = LoggerFactory.getLogger(WhatsAppServiceNoop.class);

    @Override
    public boolean enviar(Notificacao notificacao) {
        log.info("Envio por WhatsApp não implementado (MVP); notificação {} mantida pendente",
                notificacao.getIdNotificacao());
        return false;
    }
}
