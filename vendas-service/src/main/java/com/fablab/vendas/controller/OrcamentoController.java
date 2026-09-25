package com.fablab.vendas.controller;

import com.fablab.vendas.dto.OrcamentoDtos.OrcamentoAtualizacaoRequest;
import com.fablab.vendas.dto.OrcamentoDtos.OrcamentoDetalheResponse;
import com.fablab.vendas.dto.OrcamentoDtos.OrcamentoListaResponse;
import com.fablab.vendas.dto.OrcamentoDtos.OrcamentoRequest;
import com.fablab.vendas.dto.VendasPrincipal;
import com.fablab.vendas.service.OrcamentoService;
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

/** Endpoints de orçamentos (criar, ajustar, duplicar, listar com counts). */
@RestController
@RequestMapping("/orcamentos")
public class OrcamentoController {

    private static final String LEITURA = "hasAnyRole('ADMIN','BOLSISTA','VOLUNTARIO','ESTAGIARIO')";
    private static final String ESCRITA = "hasAnyRole('ADMIN','BOLSISTA','VOLUNTARIO')";

    private final OrcamentoService orcamentoService;

    public OrcamentoController(OrcamentoService orcamentoService) {
        this.orcamentoService = orcamentoService;
    }

    @PostMapping
    @PreAuthorize(ESCRITA)
    public ResponseEntity<OrcamentoDetalheResponse> criar(@Valid @RequestBody OrcamentoRequest request,
                                                          @AuthenticationPrincipal VendasPrincipal principal) {
        return ResponseEntity.status(HttpStatus.CREATED).body(orcamentoService.criar(request, principal));
    }

    @GetMapping
    @PreAuthorize(LEITURA)
    public ResponseEntity<OrcamentoListaResponse> listar(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Long clienteId) {
        return ResponseEntity.ok(orcamentoService.listar(status, clienteId));
    }

    @GetMapping("/{id}")
    @PreAuthorize(LEITURA)
    public ResponseEntity<OrcamentoDetalheResponse> detalhar(@PathVariable Long id) {
        return ResponseEntity.ok(orcamentoService.detalhar(id));
    }

    @PutMapping("/{id}")
    @PreAuthorize(ESCRITA)
    public ResponseEntity<OrcamentoDetalheResponse> atualizar(@PathVariable Long id,
                                                              @Valid @RequestBody OrcamentoAtualizacaoRequest request,
                                                              @AuthenticationPrincipal VendasPrincipal principal) {
        return ResponseEntity.ok(orcamentoService.atualizar(id, request, principal));
    }

    @PostMapping("/{id}/duplicar")
    @PreAuthorize(ESCRITA)
    public ResponseEntity<OrcamentoDetalheResponse> duplicar(@PathVariable Long id,
                                                             @AuthenticationPrincipal VendasPrincipal principal) {
        return ResponseEntity.status(HttpStatus.CREATED).body(orcamentoService.duplicar(id, principal));
    }
}
