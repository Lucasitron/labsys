package com.fablab.notification.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Propriedades do Notification Service.
 *
 * @param email     dados de remetente e destinatário padrão de e-mail
 * @param historico política de retenção do histórico
 */
@ConfigurationProperties(prefix = "notification")
public record NotificationProperties(Email email, Historico historico) {

    /** Configuração de e-mail. */
    public record Email(String remetente, String destinoPadrao) {
    }

    /** Configuração de retenção do histórico. */
    public record Historico(Integer retencaoDias) {
    }

    public int retencaoDias() {
        return historico == null || historico.retencaoDias() == null ? 90 : historico.retencaoDias();
    }

    public String remetente() {
        return email == null || email.remetente() == null ? "no-reply@fablab.local" : email.remetente();
    }

    public String destinoPadrao() {
        return email == null || email.destinoPadrao() == null ? "admin@fablab.local" : email.destinoPadrao();
    }
}
