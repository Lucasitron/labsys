package com.fablab.vendas.controller;

import com.fablab.vendas.dto.EncomendaRequest;
import com.fablab.vendas.dto.EncomendaResponse;
import com.fablab.vendas.dto.OrcamentoRequest;
import com.fablab.vendas.dto.OrcamentoResponse;
import com.fablab.vendas.dto.OrcamentoStatusRequest;
import com.fablab.vendas.service.OrcamentoService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Endpoints de orçamentos: criação, ajustes, aprovação e conversão em encomenda.
 */
@RestController
@RequestMapping("/orcamentos")
public class OrcamentoController {

    private static final String ESCRITA =
            "hasAnyRole('ADMIN','BOLSISTA','VOLUNTARIO')";

    private final OrcamentoService orcamentoService;

    public OrcamentoController(OrcamentoService orcamentoService) {
        this.orcamentoService = orcamentoService;
    }

    @PostMapping
    @PreAuthorize(ESCRITA)
    public ResponseEntity<OrcamentoResponse> criar(@Valid @RequestBody OrcamentoRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(orcamentoService.criar(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize(ESCRITA)
    public ResponseEntity<OrcamentoResponse> atualizar(@PathVariable Long id,
                                                       @Valid @RequestBody OrcamentoRequest request) {
        return ResponseEntity.ok(orcamentoService.atualizar(id, request));
    }

    @PutMapping("/{id}/status")
    @PreAuthorize(ESCRITA)
    public ResponseEntity<OrcamentoResponse> mudarStatus(@PathVariable Long id,
                                                         @Valid @RequestBody OrcamentoStatusRequest request) {
        return ResponseEntity.ok(orcamentoService.mudarStatus(id, request));
    }

    @PostMapping("/{id}/encomenda")
    @PreAuthorize(ESCRITA)
    public ResponseEntity<EncomendaResponse> converterEmEncomenda(
            @PathVariable Long id, @Valid @RequestBody EncomendaRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(orcamentoService.converterParaEncomenda(id, request));
    }
}