package com.fablab.dashboard.controller;

import com.fablab.dashboard.dto.CompleteTaskRequest;
import com.fablab.dashboard.service.CompletedTasks;
import jakarta.validation.Valid;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Conclusão de tarefas do App Shell: {@code PATCH /tasks/{id}}.
 *
 * <p>No MVP o id da task é o id do agregador (ex.: {@code rh-cert-42}); a
 * marcação de conclusão é mantida em memória. Quando o dono real da tarefa
 * (Produção/RH) existir, este endpoint deve reencaminhar a ação.</p>
 */
@RestController
@RequestMapping("/tasks")
public class TaskController {

    private final CompletedTasks completedTasks;

    public TaskController(CompletedTasks completedTasks) {
        this.completedTasks = completedTasks;
    }

    @PatchMapping("/{id}")
    public ResponseEntity<Map<String, Object>> complete(@PathVariable String id,
                                                        @Valid @RequestBody CompleteTaskRequest request) {
        boolean done = Boolean.TRUE.equals(request.done());
        if (!done) {
            return ResponseEntity.badRequest()
                    .body(Map.of("done", false, "message", "Apenas 'done: true' é suportado no MVP"));
        }
        completedTasks.mark(id);
        return ResponseEntity.ok(Map.of("done", true, "id", id));
    }
}