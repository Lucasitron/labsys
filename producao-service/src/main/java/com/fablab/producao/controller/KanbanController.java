package com.fablab.producao.controller;

import com.fablab.producao.dto.HistoricoKanbanResponse;
import com.fablab.producao.dto.KanbanMovimentoRequest;
import com.fablab.producao.dto.KanbanRequest;
import com.fablab.producao.dto.KanbanResponse;
import com.fablab.producao.entity.KanbanStatus;
import com.fablab.producao.service.KanbanService;
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

/** Endpoints do Kanban de produção. */
@RestController
@RequestMapping("/kanban")
public class KanbanController {

    private final KanbanService kanbanService;

    public KanbanController(KanbanService kanbanService) {
        this.kanbanService = kanbanService;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','BOLSISTA','VOLUNTARIO')")
    public ResponseEntity<KanbanResponse> incluir(@Valid @RequestBody KanbanRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(kanbanService.incluir(request));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','BOLSISTA','VOLUNTARIO','ESTAGIARIO')")
    public List<KanbanResponse> listar(@RequestParam(required = false) KanbanStatus status) {
        return kanbanService.listar(status);
    }

    @GetMapping("/encomenda/{idEncomenda}")
    @PreAuthorize("hasAnyRole('ADMIN','BOLSISTA','VOLUNTARIO','ESTAGIARIO')")
    public KanbanResponse buscarPorEncomenda(@PathVariable Long idEncomenda) {
        return kanbanService.buscarPorEncomenda(idEncomenda);
    }

    @GetMapping("/encomenda/{idEncomenda}/historico")
    @PreAuthorize("hasAnyRole('ADMIN','BOLSISTA','VOLUNTARIO','ESTAGIARIO')")
    public List<HistoricoKanbanResponse> historico(@PathVariable Long idEncomenda) {
        return kanbanService.historico(idEncomenda);
    }

    @PutMapping("/{id}/mover")
    @PreAuthorize("hasAnyRole('ADMIN','BOLSISTA','VOLUNTARIO')")
    public KanbanResponse mover(@PathVariable Long id, @Valid @RequestBody KanbanMovimentoRequest request) {
        return kanbanService.mover(id, request);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','BOLSISTA','VOLUNTARIO')")
    public ResponseEntity<Void> remover(@PathVariable Long id) {
        kanbanService.remover(id);
        return ResponseEntity.noContent().build();
    }
}