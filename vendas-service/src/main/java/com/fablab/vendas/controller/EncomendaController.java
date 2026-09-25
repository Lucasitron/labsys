package com.fablab.vendas.controller;

import com.fablab.vendas.dto.EncomendaDtos.EncomendaDetalheResponse;
import com.fablab.vendas.dto.EncomendaDtos.EncomendaListaResponse;
import com.fablab.vendas.dto.EncomendaDtos.EncomendaRequest;
import com.fablab.vendas.dto.EncomendaDtos.KanbanRequest;
import com.fablab.vendas.dto.EncomendaDtos.NovaOrdemRequest;
import com.fablab.vendas.dto.EncomendaDtos.StatusResponse;
import com.fablab.vendas.dto.VendasPrincipal;
import com.fablab.vendas.service.EncomendaService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Endpoints de encomendas e Kanban. */
@RestController
@RequestMapping("/encomendas")
public class EncomendaController {

    private static final String LEITURA = "hasAnyRole('ADMIN','BOLSISTA','VOLUNTARIO','ESTAGIARIO')";
    private static final String ESCRITA = "hasAnyRole('ADMIN','BOLSISTA','VOLUNTARIO')";

    private final EncomendaService encomendaService;

    public EncomendaController(EncomendaService encomendaService) {
        this.encomendaService = encomendaService;
    }

    @PostMapping
    @PreAuthorize(ESCRITA)
    public ResponseEntity<EncomendaDetalheResponse> criar(@Valid @RequestBody EncomendaRequest request,
                                                          @AuthenticationPrincipal VendasPrincipal principal) {
        return ResponseEntity.status(HttpStatus.CREATED).body(encomendaService.criar(request, principal));
    }

    @GetMapping
    @PreAuthorize(LEITURA)
    public ResponseEntity<EncomendaListaResponse> listar(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Long clienteId) {
        return ResponseEntity.ok(encomendaService.listar(status, clienteId));
    }

    @GetMapping("/{id}")
    @PreAuthorize(LEITURA)
    public ResponseEntity<EncomendaDetalheResponse> detalhar(@PathVariable Long id) {
        return ResponseEntity.ok(encomendaService.detalhar(id));
    }

    @GetMapping("/{id}/status")
    @PreAuthorize(LEITURA)
    public ResponseEntity<StatusResponse> status(@PathVariable Long id) {
        return ResponseEntity.ok(encomendaService.status(id));
    }

    @PutMapping("/{id}/kanban")
    @PreAuthorize(ESCRITA)
    public ResponseEntity<EncomendaDetalheResponse> moverKanban(@PathVariable Long id,
                                                                @Valid @RequestBody KanbanRequest request,
                                                                @AuthenticationPrincipal VendasPrincipal principal) {
        return ResponseEntity.ok(encomendaService.moverKanban(id, request, principal));
    }

    @PostMapping("/{id}/nova-ordem")
    @PreAuthorize(ESCRITA)
    public ResponseEntity<EncomendaDetalheResponse> novaOrdem(@PathVariable Long id,
                                                              @Valid @RequestBody NovaOrdemRequest request,
                                                              @AuthenticationPrincipal VendasPrincipal principal) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(encomendaService.novaOrdem(id, request, principal));
    }
}
