package com.fablab.dashboard.controller;

import com.fablab.dashboard.dto.AuthPrincipal;
import com.fablab.dashboard.dto.CompleteTaskRequest;
import com.fablab.dashboard.service.CompletedTasks;
import com.fablab.dashboard.service.DominioClient;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Conclusão de tarefas do App Shell: {@code PATCH /tasks/{id}}.
 *
 * <p>No MVP o id da task é o id do agregador (ex.: {@code rh-cert-42}); a
 * marcação de conclusão é mantida em memória. O dono real da tarefa é o
 * serviço de domínio (Produção/RH): o repoint da rota no gateway para o
 * {@code producao-service} é follow-up pendente (contrato incompatível —
 * {@code PUT /tarefas/{id}/status} com id numérico), então este endpoint
 * segue no dashboard com validação de vínculo.</p>
 */
@RestController
@RequestMapping("/tasks")
public class TaskController {

    private static final Pattern TAREFA_CERTIFICADO = Pattern.compile("^rh-cert-(\\d+)$");

    private final CompletedTasks completedTasks;
    private final DominioClient dominioClient;

    public TaskController(CompletedTasks completedTasks, DominioClient dominioClient) {
        this.completedTasks = completedTasks;
        this.dominioClient = dominioClient;
    }

    @PatchMapping("/{id}")
    public ResponseEntity<Map<String, Object>> complete(@PathVariable String id,
                                                        @Valid @RequestBody CompleteTaskRequest request,
                                                        @AuthenticationPrincipal AuthPrincipal principal,
                                                        HttpServletRequest httpRequest) {
        boolean done = Boolean.TRUE.equals(request.done());
        if (!done) {
            return ResponseEntity.badRequest()
                    .body(Map.of("done", false, "message", "Apenas 'done: true' é suportado no MVP"));
        }
        if (principal != null && !principal.isAdmin()
                && !vinculoComAutenticado(id, bearerDe(httpRequest))) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("done", false,
                            "message", "Tarefa não encontrada ou sem vínculo com o usuário autenticado"));
        }
        completedTasks.mark(id);
        return ResponseEntity.ok(Map.of("done", true, "id", id));
    }

    /**
     * Verifica se a tarefa pertence ao usuário autenticado. O RH filtra as
     * solicitações pelo vínculo do portador do token, então basta a tarefa
     * constar na lista obtida com o bearer do próprio usuário.
     */
    private boolean vinculoComAutenticado(String taskId, String bearer) {
        Matcher matcher = TAREFA_CERTIFICADO.matcher(taskId);
        if (!matcher.matches()) {
            return false;
        }
        long idSolicitacao = Long.parseLong(matcher.group(1));
        List<JsonNode> solicitacoes = dominioClient.solicitacoes(null, bearer);
        return solicitacoes.stream().anyMatch(s -> {
            JsonNode campo = s.get("idSolicitacao");
            return campo != null && campo.isNumber() && campo.longValue() == idSolicitacao;
        });
    }

    private String bearerDe(HttpServletRequest request) {
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        return header != null && header.startsWith("Bearer ") ? header.substring(7) : "";
    }
}
