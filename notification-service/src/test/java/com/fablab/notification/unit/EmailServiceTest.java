package com.fablab.notification.unit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fablab.notification.config.NotificationProperties;
import com.fablab.notification.entity.CanalNotificacao;
import com.fablab.notification.entity.Notificacao;
import com.fablab.notification.service.ConfiguracaoCanalService;
import com.fablab.notification.service.EmailService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

@ExtendWith(MockitoExtension.class)
class EmailServiceTest {

    @Mock
    private JavaMailSender mailSender;
    @Mock
    private ConfiguracaoCanalService configuracaoCanalService;

    private EmailService emailService;

    @BeforeEach
    void setUp() {
        NotificationProperties properties = new NotificationProperties(
                new NotificationProperties.Email("no-reply@fablab.local", "admin@fablab.local"),
                new NotificationProperties.Historico(90));
        emailService = new EmailService(mailSender, configuracaoCanalService, properties);
    }

    private Notificacao notificacao() {
        Notificacao notificacao = new Notificacao();
        notificacao.setIdNotificacao(1L);
        notificacao.setIdDestinatario(7L);
        notificacao.setCanal(CanalNotificacao.EMAIL);
        notificacao.setAssunto("Assunto");
        notificacao.setMensagem("Mensagem");
        return notificacao;
    }

    @Test
    void naoEnviaQuandoCanalDesabilitado() {
        when(configuracaoCanalService.isHabilitado(CanalNotificacao.EMAIL)).thenReturn(false);

        assertThat(emailService.enviar(notificacao())).isFalse();
        verify(mailSender, never()).send(any(SimpleMailMessage.class));
    }

    @Test
    void enviaEmailComRemetenteEDestinatarioPadrao() {
        when(configuracaoCanalService.isHabilitado(CanalNotificacao.EMAIL)).thenReturn(true);

        assertThat(emailService.enviar(notificacao())).isTrue();

        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(captor.capture());
        SimpleMailMessage message = captor.getValue();
        assertThat(message.getFrom()).isEqualTo("no-reply@fablab.local");
        assertThat(message.getTo()).containsExactly("admin@fablab.local");
        assertThat(message.getSubject()).isEqualTo("Assunto");
        assertThat(message.getText()).isEqualTo("Mensagem");
    }

    @Test
    void retornaFalsoQuandoEnvioFalha() {
        when(configuracaoCanalService.isHabilitado(CanalNotificacao.EMAIL)).thenReturn(true);
        org.mockito.Mockito.doThrow(new MailSendException("falha"))
                .when(mailSender).send(any(SimpleMailMessage.class));

        assertThat(emailService.enviar(notificacao())).isFalse();
    }
}
