package com.fablab.notification.unit;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

import com.fablab.notification.dto.AdvertenciaRegistradaEvent;
import com.fablab.notification.dto.CertificadoAprovadoEvent;
import com.fablab.notification.dto.CertificadoRejeitadoEvent;
import com.fablab.notification.dto.CertificadoSolicitadoEvent;
import com.fablab.notification.dto.CompraSolicitadaEvent;
import com.fablab.notification.dto.EmprestimoAtrasadoEvent;
import com.fablab.notification.dto.EncomendaCriadaEvent;
import com.fablab.notification.dto.EncomendaStatusAlteradoEvent;
import com.fablab.notification.dto.EstoqueBaixoEvent;
import com.fablab.notification.dto.ExtratoMensalHorasEvent;
import com.fablab.notification.dto.HorasValidadasEvent;
import com.fablab.notification.dto.KanbanStatusAlteradoEvent;
import com.fablab.notification.dto.LancamentoVencidoEvent;
import com.fablab.notification.dto.NivelAlteradoEvent;
import com.fablab.notification.dto.OrcamentoAprovadoEvent;
import com.fablab.notification.dto.ProjetoMesaAbandonadoEvent;
import com.fablab.notification.entity.CanalNotificacao;
import com.fablab.notification.entity.TipoEvento;
import com.fablab.notification.messaging.NotificacaoEventListener;
import com.fablab.notification.service.NotificacaoService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class NotificacaoEventListenerTest {

    @Mock
    private NotificacaoService notificacaoService;

    private NotificacaoEventListener listener;

    @BeforeEach
    void setUp() {
        listener = new NotificacaoEventListener(notificacaoService);
    }

    @Test
    void estoqueBaixoGeraNotificacaoParaAdmin() {
        listener.onEstoqueBaixo(new EstoqueBaixoEvent(1L, "Filamento", new BigDecimal("1"), new BigDecimal("5")));

        verify(notificacaoService).registrar(eq(null), eq(CanalNotificacao.EMAIL), eq(TipoEvento.ESTOQUE_BAIXO),
                anyString(), anyString(), eq(1L));
    }

    @Test
    void emprestimoAtrasadoNotificaPessoa() {
        listener.onEmprestimoAtrasado(new EmprestimoAtrasadoEvent(2L, 7L, 1L, LocalDate.now()));

        verify(notificacaoService).registrar(eq(7L), eq(CanalNotificacao.EMAIL), eq(TipoEvento.EMPRESTIMO_ATRASADO),
                anyString(), anyString(), eq(2L));
    }

    @Test
    void encomendaCriadaNotificaCliente() {
        listener.onEncomendaCriada(new EncomendaCriadaEvent(3L, 8L, "BACKLOG",
                new BigDecimal("10"), LocalDate.now()));

        verify(notificacaoService).registrar(eq(8L), eq(CanalNotificacao.EMAIL), eq(TipoEvento.ENCOMENDA_CRIADA),
                anyString(), anyString(), eq(3L));
    }

    @Test
    void encomendaStatusAlteradoNotificaCliente() {
        listener.onEncomendaStatusAlterado(new EncomendaStatusAlteradoEvent(3L, 8L, "BACKLOG", "PRONTO",
                LocalDateTime.now()));

        verify(notificacaoService).registrar(eq(8L), eq(CanalNotificacao.EMAIL),
                eq(TipoEvento.ENCOMENDA_STATUS_ALTERADO), anyString(), anyString(), eq(3L));
    }

    @Test
    void orcamentoAprovadoNotificaCliente() {
        listener.onOrcamentoAprovado(new OrcamentoAprovadoEvent(4L, 8L, new BigDecimal("99"), LocalDate.now()));

        verify(notificacaoService).registrar(eq(8L), eq(CanalNotificacao.EMAIL), eq(TipoEvento.ORCAMENTO_APROVADO),
                anyString(), anyString(), eq(4L));
    }

    @Test
    void lancamentoVencidoGeraNotificacaoParaAdmin() {
        listener.onLancamentoVencido(new LancamentoVencidoEvent(5L, "PAGAR", new BigDecimal("50"),
                LocalDate.now()));

        verify(notificacaoService).registrar(eq(null), eq(CanalNotificacao.EMAIL), eq(TipoEvento.LANCAMENTO_VENCIDO),
                anyString(), anyString(), eq(5L));
    }

    @Test
    void compraSolicitadaGeraNotificacaoParaAdmin() {
        listener.onCompraSolicitada(new CompraSolicitadaEvent(6L, 2L));

        verify(notificacaoService).registrar(eq(null), eq(CanalNotificacao.EMAIL), eq(TipoEvento.COMPRA_SOLICITADA),
                anyString(), anyString(), eq(6L));
    }

    @Test
    void advertenciaRegistradaNotificaFuncionario() {
        listener.onAdvertenciaRegistrada(new AdvertenciaRegistradaEvent(7L, 2, "Atraso"));

        verify(notificacaoService).registrar(eq(7L), eq(CanalNotificacao.EMAIL), eq(TipoEvento.ADVERTENCIA_REGISTRADA),
                anyString(), anyString(), eq(7L));
    }

    @Test
    void projetoMesaAbandonadoNotificaFuncionario() {
        listener.onProjetoMesaAbandonado(new ProjetoMesaAbandonadoEvent(9L, 7L, "Liberado"));

        verify(notificacaoService).registrar(eq(7L), eq(CanalNotificacao.EMAIL),
                eq(TipoEvento.PROJETO_MESA_ABANDONADO), anyString(), anyString(), eq(9L));
    }

    @Test
    void kanbanStatusAlteradoGeraNotificacaoParaAdmin() {
        listener.onKanbanStatusAlterado(new KanbanStatusAlteradoEvent(3L, "BACKLOG", "PRONTO",
                LocalDateTime.now()));

        verify(notificacaoService).registrar(eq(null), eq(CanalNotificacao.EMAIL),
                eq(TipoEvento.ENCOMENDA_STATUS_ALTERADO), anyString(), anyString(), eq(3L));
    }

    @Test
    void nivelAlteradoNotificaFuncionario() {
        listener.onNivelAlterado(new NivelAlteradoEvent(7L, "BOLSISTA", "ADMIN", java.time.Instant.now()));

        verify(notificacaoService).registrar(eq(7L), eq(CanalNotificacao.EMAIL), eq(TipoEvento.NIVEL_ALTERADO),
                anyString(), anyString(), eq(7L));
    }

    @Test
    void horasValidadasNotificaFuncionario() {
        listener.onHorasValidadas(new HorasValidadasEvent(7L, "PROJETO", 11L, new BigDecimal("4"),
                LocalDate.now()));

        verify(notificacaoService).registrar(eq(7L), eq(CanalNotificacao.EMAIL), eq(TipoEvento.HORAS_VALIDADAS),
                anyString(), anyString(), eq(11L));
    }

    @Test
    void certificadoSolicitadoGeraNotificacaoParaAdmin() {
        listener.onCertificadoSolicitado(new CertificadoSolicitadoEvent(1L, 7L, "COMPLEMENTAR",
                new BigDecimal("10"), LocalDateTime.now()));

        verify(notificacaoService).registrar(eq(null), eq(CanalNotificacao.EMAIL),
                eq(TipoEvento.CERTIFICADO_SOLICITADO), anyString(), anyString(), eq(1L));
    }

    @Test
    void certificadoAprovadoNotificaFuncionario() {
        listener.onCertificadoAprovado(new CertificadoAprovadoEvent(1L, 2L, 7L, "Maria", "COMPLEMENTAR",
                new BigDecimal("10"), LocalDateTime.now()));

        verify(notificacaoService).registrar(eq(7L), eq(CanalNotificacao.EMAIL),
                eq(TipoEvento.CERTIFICADO_APROVADO), eq("Certificado de horas aprovado"),
                org.mockito.ArgumentMatchers.contains("Olá Maria"), eq(2L));
    }

    @Test
    void certificadoRejeitadoNotificaFuncionarioComMotivo() {
        listener.onCertificadoRejeitado(new CertificadoRejeitadoEvent(1L, 7L, "Maria", "ESTAGIO",
                "Dados insuficientes", LocalDateTime.now()));

        verify(notificacaoService).registrar(eq(7L), eq(CanalNotificacao.EMAIL),
                eq(TipoEvento.CERTIFICADO_REJEITADO), eq("Solicitação de certificado rejeitada"),
                org.mockito.ArgumentMatchers.contains("Dados insuficientes"), eq(1L));
    }

    @Test
    void extratoMensalHorasNotificaFuncionarioComResumoFormatado() {
        listener.onExtratoMensalHoras(new ExtratoMensalHorasEvent(7L, "Maria", "08/2026",
                new BigDecimal("80.00"), new BigDecimal("20.00"), new BigDecimal("30.00"),
                new BigDecimal("120.00")));

        verify(notificacaoService).registrar(eq(7L), eq(CanalNotificacao.EMAIL),
                eq(TipoEvento.EXTRATO_MENSAL_HORAS), eq("Extrato mensal de horas"),
                org.mockito.ArgumentMatchers.contains(
                        "Olá Maria,\nSegue o extrato de horas do mês 08/2026:\n"
                                + "- Presença: 80.00h\n- Encomendas: 20.00h\n"
                                + "- Projetos: 30.00h\n"
                                + "- Total disponível para certificado: 120.00h"),
                eq(7L));
    }

    @Test
    void formatarExtratoMensalGeraResumoLegivel() {
        String resumo = NotificacaoEventListener.formatarExtratoMensal(new ExtratoMensalHorasEvent(
                7L, "Maria", "08/2026",
                new BigDecimal("80.00"), new BigDecimal("20.00"), new BigDecimal("30.00"),
                new BigDecimal("120.00")));

        org.assertj.core.api.Assertions.assertThat(resumo)
                .contains("Olá Maria")
                .contains("mês 08/2026")
                .contains("- Presença: 80.00h")
                .contains("- Encomendas: 20.00h")
                .contains("- Projetos: 30.00h")
                .contains("- Total disponível para certificado: 120.00h");
    }
}
