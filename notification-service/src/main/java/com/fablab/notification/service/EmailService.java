package com.fablab.notification.service;

import com.fablab.notification.config.NotificationProperties;
import com.fablab.notification.entity.CanalNotificacao;
import com.fablab.notification.entity.Notificacao;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

/**
 * Envio de e-mail via {@link JavaMailSender}. O remetente e o destinatário
 * padrão vêm de {@code notification.email}; o canal só envia se estiver
 * habilitado em {@code ConfiguracaoCanal}.
 */
@Service
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    private final JavaMailSender mailSender;
    private final ConfiguracaoCanalService configuracaoCanalService;
    private final NotificationProperties properties;

    public EmailService(JavaMailSender mailSender,
                        ConfiguracaoCanalService configuracaoCanalService,
                        NotificationProperties properties) {
        this.mailSender = mailSender;
        this.configuracaoCanalService = configuracaoCanalService;
        this.properties = properties;
    }

    /**
     * Tenta enviar a notificação por e-mail.
     *
     * @return {@code true} se enviado; {@code false} se o canal está desabilitado
     *         ou houve falha no envio
     */
    public boolean enviar(Notificacao notificacao) {
        if (!configuracaoCanalService.isHabilitado(CanalNotificacao.EMAIL)) {
            log.warn("Canal EMAIL desabilitado; notificação {} permanece pendente",
                    notificacao.getIdNotificacao());
            return false;
        }
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(properties.remetente());
            message.setTo(properties.destinoPadrao());
            message.setSubject(notificacao.getAssunto());
            message.setText(notificacao.getMensagem());
            mailSender.send(message);
            log.info("E-mail enviado para a notificação {}", notificacao.getIdNotificacao());
            return true;
        } catch (MailException ex) {
            log.error("Falha ao enviar e-mail da notificação {}: {}",
                    notificacao.getIdNotificacao(), ex.getMessage());
            return false;
        }
    }
}
