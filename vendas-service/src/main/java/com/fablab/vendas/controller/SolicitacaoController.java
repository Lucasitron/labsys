package com.fablab.vendas.controller;

import com.fablab.vendas.dto.SolicitacaoDtos.DecisaoRequest;
import com.fablab.vendas.dto.SolicitacaoDtos.SolicitacaoListaResponse;
import com.fablab.vendas.dto.SolicitacaoDtos.SolicitacaoRequest;
import com.fablab.vendas.dto.SolicitacaoDtos.SolicitacaoResponse;
import com.fablab.vendas.dto.VendasPrincipal;
import com.fablab.vendas.service.SolicitacaoService;
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

/**
 * Solicitações de edição (contrato mínimo — D-4 REPORTADO): solicitar e
 * listar por qualquer usuário; decisão só Admin.
 */
@RestController
@RequestMapping("/solicitacoes")
public class SolicitacaoController {

    private static final String ACESSO = "hasAnyRole('ADMIN','BOLSISTA','VOLUNTARIO','ESTAGIARIO')";

    private final SolicitacaoService solicitacaoService;

    public SolicitacaoController(SolicitacaoService solicitacaoService) {
        this.solicitacaoService = solicitacaoService;
    }

    @PostMapping
    @PreAuthorize(ACESSO)
    public ResponseEntity<SolicitacaoResponse> solicitar(@Valid @RequestBody SolicitacaoRequest request,
                                                         @AuthenticationPrincipal VendasPrincipal principal) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(solicitacaoService.solicitar(request, principal));
    }

    @GetMapping
    @PreAuthorize(ACESSO)
    public ResponseEntity<SolicitacaoListaResponse> listar(
            @RequestParam(required = false) String status) {
        return ResponseEntity.ok(solicitacaoService.listar(status));
    }

    @PutMapping("/{id}/decisao")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<SolicitacaoResponse> decidir(@PathVariable Long id,
                                                       @Valid @RequestBody DecisaoRequest request,
                                                       @AuthenticationPrincipal VendasPrincipal principal) {
        return ResponseEntity.ok(solicitacaoService.decidir(id, request, principal));
    }
}
