package com.fablab.dashboard.unit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fablab.dashboard.dto.AuthPrincipal;
import com.fablab.dashboard.dto.DashboardSummary;
import com.fablab.dashboard.service.CompletedTasks;
import com.fablab.dashboard.service.DashboardService;
import com.fablab.dashboard.service.DominioClient;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Agregação do resumo a partir das solicitações do RH.
 */
class DashboardServiceTest {

    private final ObjectMapper mapper = new ObjectMapper();

    private DominioClient dominioClient;
    private CompletedTasks completedTasks;
    private DashboardService service;

    @BeforeEach
    void setUp() {
        dominioClient = mock(DominioClient.class);
        completedTasks = new CompletedTasks();
        service = new DashboardService(dominioClient, completedTasks);
    }

    private JsonNode solicitacao(long id, String status) throws Exception {
        return mapper.readTree("""
                {"idSolicitacao": %d, "status": "%s", "nomeFuncionario": "Ana Souza",
                 "tipoCertificado": "Treinamento", "horasSolicitadas": 8,
                 "dataSolicitacao": "2026-09-18T10:00:00"}""".formatted(id, status));
    }

    @Test
    void resumoUsuarioComumFiltraPendentes() throws Exception {
        when(dominioClient.solicitacoes(null, "t")).thenReturn(List.of(
                solicitacao(1, "PENDENTE"), solicitacao(2, "EMITIDO")));
        when(dominioClient.unreadCount("t")).thenReturn(3);

        DashboardSummary summary = service.resumo(
                new AuthPrincipal(7L, "joao", "VOLUNTARIO", "marketing"), "t");

        assertThat(summary.tasks()).hasSize(1);
        assertThat(summary.tasks().get(0).id()).isEqualTo("rh-cert-1");
        assertThat(summary.tasks().get(0).title()).contains("aguardando aprovação");
        assertThat(summary.tasks().get(0).module()).isEqualTo("rh");
        assertThat(summary.tasks().get(0).dueLabel()).isEqualTo("Hoje");
        assertThat(summary.tasks().get(0).urgent()).isTrue();
        assertThat(summary.kpis().get("ordersActive").value()).isZero();
        assertThat(summary.kpis().get("notificationsUnread").value()).isEqualTo(3);
        assertThat(summary.activity()).isNotEmpty();
    }

    @Test
    void resumoAdminRevisaTodasAsPendentes() throws Exception {
        when(dominioClient.solicitacoes("PENDENTE", "t")).thenReturn(List.of(
                solicitacao(1, "PENDENTE"), solicitacao(2, "PENDENTE")));
        when(dominioClient.emitidos("t")).thenReturn(List.of(mapper.readTree(
                """
                {"idCertificado": 9, "nomeFuncionario": "Carlos", "dataEmissao": "2026-09-17"}""")));
        when(dominioClient.unreadCount("t")).thenReturn(0);

        DashboardSummary summary = service.resumo(
                new AuthPrincipal(1L, "admin", "ADMIN", null), "t");

        assertThat(summary.tasks()).hasSize(2);
        assertThat(summary.tasks().get(0).title()).startsWith("Revisar solicitação");
        assertThat(summary.activity()).anyMatch(item -> item.id().startsWith("act-emit-"));
    }

    @Test
    void tarefaConcluidaNaoAparece() throws Exception {
        completedTasks.mark("rh-cert-1");
        when(dominioClient.solicitacoes("PENDENTE", "t")).thenReturn(List.of(
                solicitacao(1, "PENDENTE"), solicitacao(2, "PENDENTE")));

        DashboardSummary summary = service.resumo(
                new AuthPrincipal(1L, "admin", "ADMIN", null), "t");

        assertThat(summary.tasks()).extracting("id").containsExactly("rh-cert-2");
    }

    @Test
    void degradaQuandoRhIndisponivel() {
        when(dominioClient.solicitacoes("PENDENTE", "t")).thenReturn(List.of());
        when(dominioClient.unreadCount("t")).thenReturn(0);

        DashboardSummary summary = service.resumo(
                new AuthPrincipal(1L, "admin", "ADMIN", null), "t");

        assertThat(summary.tasks()).isEmpty();
        assertThat(summary.activity()).isEmpty();
        assertThat(summary.kpis().get("ordersActive").value()).isZero();
    }
}