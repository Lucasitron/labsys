package com.fablab.notification.unit;

import static org.assertj.core.api.Assertions.assertThat;

import com.fablab.notification.entity.CanalNotificacao;
import com.fablab.notification.entity.Notificacao;
import com.fablab.notification.service.WhatsAppServiceNoop;
import org.junit.jupiter.api.Test;

class WhatsAppServiceNoopTest {

    private final WhatsAppServiceNoop service = new WhatsAppServiceNoop();

    @Test
    void envioNaoImplementadoRetornaFalso() {
        Notificacao notificacao = new Notificacao();
        notificacao.setIdNotificacao(1L);
        notificacao.setCanal(CanalNotificacao.WHATSAPP);

        assertThat(service.enviar(notificacao)).isFalse();
    }
}
