package com.fablab.notification.service;

import com.fablab.notification.entity.Notificacao;

/**
 * Contrato de envio por WhatsApp. A implementação no MVP é um no-op, mantendo a
 * estrutura pronta para integração futura.
 */
public interface WhatsAppService {

    /**
     * Tenta enviar a notificação por WhatsApp.
     *
     * @return {@code true} se enviada; {@code false} caso contrário
     */
    boolean enviar(Notificacao notificacao);
}
