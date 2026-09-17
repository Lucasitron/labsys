package com.fablab.notification.unit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fablab.notification.config.NotificationProperties;
import com.fablab.notification.dto.NotificacaoResponse;
import com.fablab.notification.dto.NotificacaoTesteRequest;
import com.fablab.notification.entity.CanalNotificacao;
import com.fablab.notification.entity.Notificacao;
import com.fablab.notification.entity.NotificacaoHistorico;
import com.fablab.notification.entity.StatusNotificacao;
import com.fablab.notification.entity.TipoEvento;
import com.fablab.notification.exception.ForbiddenException;
import com.fablab.notification.exception.ResourceNotFoundException;
import com.fablab.notification.repository.NotificacaoHistoricoRepository;
import com.fablab.notification.repository.NotificacaoRepository;
import com.fablab.notification.service.AcessoService;
import com.fablab.notification.service.ConfiguracaoCanalService;
import com.fablab.notification.service.EmailService;
import com.fablab.notification.service.NotificacaoService;
import com.fablab.notification.service.WhatsAppService;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class NotificacaoServiceTest {

    @Mock
    private NotificacaoRepository notificacaoRepository;
    @Mock
    private NotificacaoHistoricoRepository historicoRepository;
    @Mock
    private EmailService emailService;
    @Mock
    private WhatsAppService whatsAppService;
    @Mock
    private ConfiguracaoCanalService configuracaoCanalService;
    @Mock
    private AcessoService acessoService;

    private NotificacaoService service;

    @BeforeEach
    void setUp() {
        NotificationProperties properties = new NotificationProperties(
                new NotificationProperties.Email("no-reply@fablab.local", "admin@fablab.local"),
                new NotificationProperties.Historico(90));
        service = new NotificacaoService(notificacaoRepository, historicoRepository, emailService,
                whatsAppService, configuracaoCanalService, acessoService, properties);
        lenient().when(notificacaoRepository.save(any(Notificacao.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void registrarEmailEnviadoMarcaComoEnviada() {
        when(emailService.enviar(any(Notificacao.class))).thenReturn(true);

        NotificacaoResponse response = service.registrar(7L, TipoEvento.ESTOQUE_BAIXO,
                "Assunto", "Mensagem", 3L);

        assertThat(response.status()).isEqualTo(StatusNotificacao.ENVIADA);
        assertThat(response.dataEnvio()).isNotNull();
        assertThat(response.idDestinatario()).isEqualTo(7L);
        assertThat(response.canal()).isEqualTo(CanalNotificacao.EMAIL);
    }

    @Test
    void registrarEmailComFalhaMantemPendente() {
        when(emailService.enviar(any(Notificacao.class))).thenReturn(false);

        NotificacaoResponse response = service.registrar(7L, TipoEvento.ESTOQUE_BAIXO,
                "Assunto", "Mensagem", 3L);

        assertThat(response.status()).isEqualTo(StatusNotificacao.PENDENTE);
        assertThat(response.dataEnvio()).isNull();
    }

    @Test
    void registrarWhatsappNaoImplementadoMantemPendente() {
        when(whatsAppService.enviar(any(Notificacao.class))).thenReturn(false);

        NotificacaoResponse response = service.registrar(7L, CanalNotificacao.WHATSAPP,
                TipoEvento.TESTE, "Assunto", "Mensagem", null);

        assertThat(response.status()).isEqualTo(StatusNotificacao.PENDENTE);
        verify(whatsAppService).enviar(any(Notificacao.class));
        verify(emailService, never()).enviar(any(Notificacao.class));
    }

    @Test
    void listarDoUsuarioSemAutenticacaoLancaForbidden() {
        when(acessoService.idUsuario()).thenReturn(null);

        assertThatThrownBy(() -> service.listarDoUsuario(null, null))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void listarDoUsuarioComFiltrosDelegaAoRepositorio() {
        when(acessoService.idUsuario()).thenReturn(7L);
        when(notificacaoRepository.findByIdDestinatarioAndStatusAndTipoEventoOrderByDataCriacaoDesc(
                7L, StatusNotificacao.PENDENTE, TipoEvento.TESTE)).thenReturn(List.of(notificacao()));

        List<NotificacaoResponse> resultado = service.listarDoUsuario(StatusNotificacao.PENDENTE,
                TipoEvento.TESTE);

        assertThat(resultado).hasSize(1);
    }

    @Test
    void listarDoUsuarioSemFiltrosDelegaAoRepositorio() {
        when(acessoService.idUsuario()).thenReturn(7L);
        when(notificacaoRepository.findByIdDestinatarioOrderByDataCriacaoDesc(7L))
                .thenReturn(List.of(notificacao()));

        assertThat(service.listarDoUsuario(null, null)).hasSize(1);
    }

    @Test
    void listarDoUsuarioApenasStatusDelegaAoRepositorio() {
        when(acessoService.idUsuario()).thenReturn(7L);
        when(notificacaoRepository.findByIdDestinatarioAndStatusOrderByDataCriacaoDesc(
                7L, StatusNotificacao.LIDA)).thenReturn(List.of());

        assertThat(service.listarDoUsuario(StatusNotificacao.LIDA, null)).isEmpty();
    }

    @Test
    void listarDoUsuarioApenasTipoDelegaAoRepositorio() {
        when(acessoService.idUsuario()).thenReturn(7L);
        when(notificacaoRepository.findByIdDestinatarioAndTipoEventoOrderByDataCriacaoDesc(
                7L, TipoEvento.TESTE)).thenReturn(List.of());

        assertThat(service.listarDoUsuario(null, TipoEvento.TESTE)).isEmpty();
    }

    @Test
    void listarAdminComFiltrosDelegaAoRepositorio() {
        when(notificacaoRepository.findByStatusAndTipoEventoOrderByDataCriacaoDesc(
                StatusNotificacao.PENDENTE, TipoEvento.TESTE)).thenReturn(List.of(notificacao()));
        when(notificacaoRepository.findByStatusOrderByDataCriacaoDesc(StatusNotificacao.PENDENTE))
                .thenReturn(List.of());
        when(notificacaoRepository.findByTipoEventoOrderByDataCriacaoDesc(TipoEvento.TESTE))
                .thenReturn(List.of());
        when(notificacaoRepository.findAllByOrderByDataCriacaoDesc()).thenReturn(List.of());

        assertThat(service.listarAdmin(StatusNotificacao.PENDENTE, TipoEvento.TESTE)).hasSize(1);
        assertThat(service.listarAdmin(StatusNotificacao.PENDENTE, null)).isEmpty();
        assertThat(service.listarAdmin(null, TipoEvento.TESTE)).isEmpty();
        assertThat(service.listarAdmin(null, null)).isEmpty();
    }

    @Test
    void marcarComoLidaDoProprioUsuario() {
        Notificacao notificacao = notificacao();
        notificacao.setIdNotificacao(1L);
        when(notificacaoRepository.findById(1L)).thenReturn(Optional.of(notificacao));
        when(acessoService.podeAcessar(7L)).thenReturn(true);

        NotificacaoResponse response = service.marcarComoLida(1L);

        assertThat(response.status()).isEqualTo(StatusNotificacao.LIDA);
        assertThat(response.dataLeitura()).isNotNull();
    }

    @Test
    void marcarComoLidaInexistenteLancaNotFound() {
        when(notificacaoRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.marcarComoLida(99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void marcarComoLidaDeOutroUsuarioLancaForbidden() {
        Notificacao notificacao = notificacao();
        notificacao.setIdNotificacao(1L);
        when(notificacaoRepository.findById(1L)).thenReturn(Optional.of(notificacao));
        when(acessoService.podeAcessar(7L)).thenReturn(false);

        assertThatThrownBy(() -> service.marcarComoLida(1L))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void marcarComoLidaJaLidaMantemDataLeitura() {
        Notificacao notificacao = notificacao();
        notificacao.setIdNotificacao(1L);
        LocalDateTime leitura = LocalDateTime.now().minusHours(2);
        notificacao.setStatus(StatusNotificacao.LIDA);
        notificacao.setDataLeitura(leitura);
        when(notificacaoRepository.findById(1L)).thenReturn(Optional.of(notificacao));
        when(acessoService.podeAcessar(7L)).thenReturn(true);

        NotificacaoResponse response = service.marcarComoLida(1L);

        assertThat(response.dataLeitura()).isEqualTo(leitura);
    }

    @Test
    void revisarMoveParaHistoricoEDeleta() {
        Notificacao notificacao = notificacao();
        notificacao.setIdNotificacao(1L);
        when(acessoService.idUsuario()).thenReturn(42L);
        when(notificacaoRepository.findById(1L)).thenReturn(Optional.of(notificacao));

        int total = service.revisar(List.of(1L));

        assertThat(total).isEqualTo(1);
        ArgumentCaptor<NotificacaoHistorico> captor = ArgumentCaptor.forClass(NotificacaoHistorico.class);
        verify(historicoRepository).save(captor.capture());
        assertThat(captor.getValue().getIdNotificacaoOriginal()).isEqualTo(1L);
        assertThat(captor.getValue().getIdAdminRevisor()).isEqualTo(42L);
        assertThat(captor.getValue().getDataRevisaoAdmin()).isNotNull();
        verify(notificacaoRepository).delete(notificacao);
    }

    @Test
    void listarHistoricoComFiltrosDelegaAoRepositorio() {
        when(historicoRepository.findByCanalAndTipoEventoOrderByDataRevisaoAdminDesc(
                CanalNotificacao.EMAIL, TipoEvento.TESTE)).thenReturn(List.of(historico()));
        when(historicoRepository.findByCanalOrderByDataRevisaoAdminDesc(CanalNotificacao.EMAIL))
                .thenReturn(List.of());
        when(historicoRepository.findByTipoEventoOrderByDataRevisaoAdminDesc(TipoEvento.TESTE))
                .thenReturn(List.of());
        when(historicoRepository.findAllByOrderByDataRevisaoAdminDesc()).thenReturn(List.of());

        assertThat(service.listarHistorico(CanalNotificacao.EMAIL, TipoEvento.TESTE)).hasSize(1);
        assertThat(service.listarHistorico(CanalNotificacao.EMAIL, null)).isEmpty();
        assertThat(service.listarHistorico(null, TipoEvento.TESTE)).isEmpty();
        assertThat(service.listarHistorico(null, null)).isEmpty();
    }

    @Test
    void enviarTesteComValoresPadraoUsaUsuarioAtualEEmail() {
        when(acessoService.idUsuario()).thenReturn(7L);
        when(emailService.enviar(any(Notificacao.class))).thenReturn(true);

        NotificacaoResponse response = service.enviarTeste(new NotificacaoTesteRequest(null, null, null, null));

        assertThat(response.idDestinatario()).isEqualTo(7L);
        assertThat(response.canal()).isEqualTo(CanalNotificacao.EMAIL);
        assertThat(response.tipoEvento()).isEqualTo(TipoEvento.TESTE);
        assertThat(response.assunto()).isEqualTo("Teste de notificação");
    }

    @Test
    void enviarTesteComValoresInformadosRespeitaRequest() {
        when(whatsAppService.enviar(any(Notificacao.class))).thenReturn(false);

        NotificacaoResponse response = service.enviarTeste(
                new NotificacaoTesteRequest(9L, CanalNotificacao.WHATSAPP, "Oi", "Tudo bem?"));

        assertThat(response.idDestinatario()).isEqualTo(9L);
        assertThat(response.assunto()).isEqualTo("Oi");
    }

    @Test
    void expurgarHistoricoUsaRetencaoConfigurada() {
        when(historicoRepository.deleteByDataRevisaoAdminBefore(any(LocalDateTime.class))).thenReturn(5L);

        assertThat(service.expurgarHistorico()).isEqualTo(5);
        verify(historicoRepository).deleteByDataRevisaoAdminBefore(any(LocalDateTime.class));
    }

    @Test
    void canalHabilitadoDelegaParaConfiguracao() {
        when(configuracaoCanalService.isHabilitado(CanalNotificacao.EMAIL)).thenReturn(true);

        assertThat(service.canalHabilitado(CanalNotificacao.EMAIL)).isTrue();
    }

    @Test
    void reenviarPendentesProcessaTodasAsPendentes() {
        when(notificacaoRepository.findByStatusOrderByDataCriacaoDesc(StatusNotificacao.PENDENTE))
                .thenReturn(List.of(notificacao(), notificacao()));
        when(emailService.enviar(any(Notificacao.class))).thenReturn(false);

        assertThat(service.reenviarPendentes()).isEqualTo(2);
        verify(emailService, org.mockito.Mockito.times(2)).enviar(any(Notificacao.class));
    }

    private Notificacao notificacao() {
        Notificacao notificacao = new Notificacao();
        notificacao.setIdDestinatario(7L);
        notificacao.setCanal(CanalNotificacao.EMAIL);
        notificacao.setTipoEvento(TipoEvento.TESTE);
        notificacao.setAssunto("Assunto");
        notificacao.setMensagem("Mensagem");
        notificacao.setStatus(StatusNotificacao.PENDENTE);
        notificacao.setDataCriacao(LocalDateTime.now());
        return notificacao;
    }

    private NotificacaoHistorico historico() {
        NotificacaoHistorico historico = new NotificacaoHistorico();
        historico.setIdHistorico(1L);
        historico.setCanal(CanalNotificacao.EMAIL);
        historico.setTipoEvento(TipoEvento.TESTE);
        historico.setAssunto("Assunto");
        historico.setMensagem("Mensagem");
        historico.setDataCriacao(LocalDateTime.now());
        historico.setDataRevisaoAdmin(LocalDateTime.now());
        return historico;
    }
}
