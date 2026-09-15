package com.fablab.vendas.controller;

import com.fablab.vendas.dto.EncomendaRequest;
import com.fablab.vendas.dto.EncomendaResponse;
import com.fablab.vendas.dto.HistoricoResponse;
import com.fablab.vendas.dto.KanbanRequest;
import com.fablab.vendas.entity.StatusKanban;
import com.fablab.vendas.service.EncomendaService;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
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
 * Endpoints de encomendas e o Kanban de status.
 */
@RestController
@RequestMapping("/encomendas")
public class EncomendaController {

    private static final String VISUALIZACAO =
            "hasAnyRole('ADMIN','BOLSISTA','VOLUNTARIO','ESTAGIARIO')";

    private static final String ESCRITA =
            "hasAnyRole('ADMIN','BOLSISTA','VOLUNTARIO')";

    private final EncomendaService encomendaService;

    public EncomendaController(EncomendaService encomendaService) {
        this.encomendaService = encomendaService;
    }

    @PostMapping
    @PreAuthorize(ESCRITA)
    public ResponseEntity<EncomendaResponse> criar(@Valid @RequestBody EncomendaRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(encomendaService.criar(request));
    }

    @GetMapping
    @PreAuthorize(VISUALIZACAO)
    public ResponseEntity<List<EncomendaResponse>> listar(
            @RequestParam(required = false) StatusKanban status,
            @RequestParam(required = false) Long idCliente,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataInicio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataFim) {
        return ResponseEntity.ok(encomendaService.listar(status, idCliente, dataInicio, dataFim));
    }

    @GetMapping("/{id}")
    @PreAuthorize(VISUALIZACAO)
    public ResponseEntity<EncomendaResponse> buscar(@PathVariable Long id) {
        return ResponseEntity.ok(encomendaService.buscar(id));
    }

    @PutMapping("/{id}/kanban")
    @PreAuthorize(ESCRITA)
    public ResponseEntity<EncomendaResponse> moverKanban(@PathVariable Long id,
                                                         @Valid @RequestBody KanbanRequest request) {
        return ResponseEntity.ok(encomendaService.moverKanban(id, request));
    }

    @GetMapping("/{id}/historico")
    @PreAuthorize(VISUALIZACAO)
    public ResponseEntity<List<HistoricoResponse>> listarHistorico(@PathVariable Long id) {
        return ResponseEntity.ok(encomendaService.listarHistorico(id));
    }
}