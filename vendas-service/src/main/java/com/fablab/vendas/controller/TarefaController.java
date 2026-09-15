package com.fablab.vendas.controller;

import com.fablab.vendas.dto.TarefaRequest;
import com.fablab.vendas.dto.TarefaResponse;
import com.fablab.vendas.entity.StatusTarefa;
import com.fablab.vendas.service.TarefaService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Endpoints de tarefas internas de marketing.
 */
@RestController
@RequestMapping("/tarefas-marketing")
public class TarefaController {

    private static final String VISUALIZACAO =
            "hasAnyRole('ADMIN','BOLSISTA','VOLUNTARIO','ESTAGIARIO')";

    private static final String ESCRITA =
            "hasAnyRole('ADMIN','BOLSISTA','VOLUNTARIO')";

    private final TarefaService tarefaService;

    public TarefaController(TarefaService tarefaService) {
        this.tarefaService = tarefaService;
    }

    @PostMapping
    @PreAuthorize(ESCRITA)
    public ResponseEntity<TarefaResponse> criar(@Valid @RequestBody TarefaRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(tarefaService.criar(request));
    }

    @GetMapping
    @PreAuthorize(VISUALIZACAO)
    public ResponseEntity<List<TarefaResponse>> listar(
            @RequestParam(required = false) Long idResponsavel,
            @RequestParam(required = false) StatusTarefa status) {
        return ResponseEntity.ok(tarefaService.listar(idResponsavel, status));
    }

    @PutMapping("/{id}")
    @PreAuthorize(ESCRITA)
    public ResponseEntity<TarefaResponse> atualizar(@PathVariable Long id,
                                                    @Valid @RequestBody TarefaRequest request) {
        return ResponseEntity.ok(tarefaService.atualizar(id, request));
    }

    @PutMapping("/{id}/status")
    @PreAuthorize(ESCRITA)
    public ResponseEntity<TarefaResponse> atualizarStatus(@PathVariable Long id,
                                                          @RequestBody StatusTarefa status) {
        return ResponseEntity.ok(tarefaService.atualizarStatus(id, status));
    }
}