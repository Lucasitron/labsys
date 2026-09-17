package com.fablab.notification.unit;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fablab.notification.service.ExpurgoHistoricoScheduler;
import com.fablab.notification.service.NotificacaoService;
import com.fablab.notification.service.ReenvioPendentesScheduler;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SchedulerTest {

    @Mock
    private NotificacaoService notificacaoService;

    @Test
    void expurgoInvocaServico() {
        when(notificacaoService.expurgarHistorico()).thenReturn(3);

        new ExpurgoHistoricoScheduler(notificacaoService).expurgar();

        verify(notificacaoService).expurgarHistorico();
    }

    @Test
    void reenvioInvocaServico() {
        when(notificacaoService.reenviarPendentes()).thenReturn(2);

        new ReenvioPendentesScheduler(notificacaoService).reenviar();

        verify(notificacaoService).reenviarPendentes();
    }
}
