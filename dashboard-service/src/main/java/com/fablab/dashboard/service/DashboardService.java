package com.fablab.dashboard.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fablab.dashboard.dto.ActivityItemDto;
import com.fablab.dashboard.dto.AuthPrincipal;
import com.fablab.dashboard.dto.DashboardSummary;
import com.fablab.dashboard.dto.KpiDto;
import com.fablab.dashboard.dto.TaskDto;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

/**
 * Monta o resumo do dashboard agregando os serviços de domínio (no MVP,
 * primordialmente o Pessoas &amp; RH). Fracassos de consulta degradam para
 * listas vazias/zeros.
 */
@Service
public class DashboardService {

    private final DominioClient dominioClient;
    private final CompletedTasks completedTasks;

    public DashboardService(DominioClient dominioClient, CompletedTasks completedTasks) {
        this.dominioClient = dominioClient;
        this.completedTasks = completedTasks;
    }

    public DashboardSummary resumo(AuthPrincipal principal, String bearer) {
        boolean admin = principal.isAdmin();

        List<JsonNode> solicitacoes = dominioClient.solicitacoes(admin ? "PENDENTE" : null, bearer);
        List<JsonNode> emitidos = admin ? dominioClient.emitidos(bearer) : List.of();

        List<TaskDto> tasks = new ArrayList<>();
        for (JsonNode solicitacao : solicitacoes) {
            if (!admin && !"PENDENTE".equals(text(solicitacao, "status"))) {
                continue;
            }
            TaskDto task = toTask(solicitacao, admin);
            if (!completedTasks.isDone(task.id())) {
                tasks.add(task);
            }
        }

        Map<String, KpiDto> kpis = new LinkedHashMap<>();
        kpis.put("ordersActive", new KpiDto(0, null, null, "muted", false));
        kpis.put("notificationsUnread",
                new KpiDto(dominioClient.unreadCount(bearer), null, null, "muted", false));

        List<ActivityItemDto> activity = java.util.stream.Stream.concat(
                        solicitacoes.stream().map(this::solicitacaoActivity),
                        emitidos.stream().map(this::emitidoActivity))
                .sorted(Comparator.comparing(ActivityItemDto::at, Comparator.reverseOrder()))
                .limit(8)
                .toList();

        return new DashboardSummary(tasks, kpis, List.of(), List.of(), activity);
    }

    private TaskDto toTask(JsonNode solicitacao, boolean admin) {
        String nome = text(solicitacao, "nomeFuncionario");
        String tipo = text(solicitacao, "tipoCertificado");
        String horas = text(solicitacao, "horasSolicitadas");

        String dueIso = dueIso(text(solicitacao, "dataSolicitacao"));
        LocalDate hoje = LocalDate.now();
        String dueLabel = "Sem prazo";
        boolean urgent = false;
        if (dueIso != null) {
            LocalDate due = LocalDate.parse(dueIso);
            urgent = due.equals(hoje);
            dueLabel = urgent ? "Hoje" : due.format(DateTimeFormatter.ofPattern("dd/MM"));
        }

        String title = admin
                ? "Revisar solicitação de certificado de " + nome + " (" + tipo + " · " + horas + "h)"
                : "Certificado de horas " + tipo + " aguardando aprovação";

        return new TaskDto(
                "rh-cert-" + num(solicitacao, "idSolicitacao"),
                title,
                "rh",
                dueIso,
                dueLabel,
                urgent);
    }

    private ActivityItemDto solicitacaoActivity(JsonNode solicitacao) {
        return new ActivityItemDto(
                "act-sol-" + num(solicitacao, "idSolicitacao"),
                text(solicitacao, "nomeFuncionario"),
                "solicitou certificado de horas por",
                text(solicitacao, "horasSolicitadas") + " h (" + text(solicitacao, "tipoCertificado") + ")",
                "rh",
                text(solicitacao, "dataSolicitacao"));
    }

    private ActivityItemDto emitidoActivity(JsonNode emitido) {
        return new ActivityItemDto(
                "act-emit-" + num(emitido, "idCertificado"),
                "Sistema RH",
                "emitiu certificado para",
                text(emitido, "nomeFuncionario"),
                "rh",
                text(emitido, "dataEmissao"));
    }

    private String dueIso(String dataHora) {
        if (dataHora == null || dataHora.length() < 10) {
            return null;
        }
        return dataHora.substring(0, 10);
    }

    private static String text(JsonNode node, String field) {
        JsonNode value = node.get(field);
        return value == null || value.isNull() ? null : value.asText();
    }

    private static String num(JsonNode node, String field) {
        JsonNode value = node.get(field);
        if (value == null) {
            return "0";
        }
        return value.isNumber() ? String.valueOf(value.longValue()) : value.asText();
    }
}