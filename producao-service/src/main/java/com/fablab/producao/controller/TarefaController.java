package com.fablab.producao.controller;

import com.fablab.producao.dto.TarefaRequest;
import com.fablab.producao.dto.TarefaResponse;
import com.fablab.producao.dto.TarefaStatusRequest;
import com.fablab.producao.entity.TarefaStatus;
import com.fablab.producao.service.TarefaService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Endpoints de tarefas. */
@RestController
@RequestMapping("/tarefas")
public class TarefaController {

    private final TarefaService tarefaService;

    public TarefaController(TarefaService tarefaService) {
        this.tarefaService = tarefaService;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','BOLSISTA','VOLUNTARIO')")
    public ResponseEntity<TarefaResponse> criar(@Valid @RequestBody TarefaRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(tarefaService.criar(request));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','BOLSISTA','VOLUNTARIO','ESTAGIARIO')")
    public List<TarefaResponse> listar(@RequestParam(required = false) Long idProjeto,
                                       @RequestParam(required = false) Long idResponsavel,
                                       @RequestParam(required = false) TarefaStatus status) {
        return tarefaService.listar(idProjeto, idResponsavel, status);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','BOLSISTA','VOLUNTARIO','ESTAGIARIO')")
    public TarefaResponse buscar(@PathVariable Long id) {
        return tarefaService.buscar(id);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','BOLSISTA','VOLUNTARIO')")
    public TarefaResponse atualizar(@PathVariable Long id, @Valid @RequestBody TarefaRequest request) {
        return tarefaService.atualizar(id, request);
    }

    @PutMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('ADMIN','BOLSISTA','VOLUNTARIO')")
    public TarefaResponse alterarStatus(@PathVariable Long id, @Valid @RequestBody TarefaStatusRequest request) {
        return tarefaService.alterarStatus(id, request);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','BOLSISTA','VOLUNTARIO')")
    public ResponseEntity<Void> remover(@PathVariable Long id) {
        tarefaService.remover(id);
        return ResponseEntity.noContent().build();
    }
}