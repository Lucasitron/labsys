package com.fablab.dashboard.service;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;

/**
 * Estado em memória das tarefas concluídas no App Shell (MVP, instância única).
 *
 * <p>O dono real das tarefas de produção é o {@code producao-service}
 * ({@code PUT /tarefas/{id}/status}); enquanto o repoint da rota no gateway
 * for incompatível com este contrato (follow-up), o dashboard mantém apenas
 * o conjunto de ids concluídos para ocultá-los do resumo.</p>
 */
@Component
public class CompletedTasks {

    private final Set<String> doneIds = ConcurrentHashMap.newKeySet();

    public boolean isDone(String taskId) {
        return doneIds.contains(taskId);
    }

    public void mark(String taskId) {
        doneIds.add(taskId);
    }
}