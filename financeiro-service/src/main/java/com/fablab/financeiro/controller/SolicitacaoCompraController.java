package com.fablab.financeiro.controller;

import com.fablab.financeiro.dto.SolicitacaoCompraRequest;
import com.fablab.financeiro.dto.SolicitacaoCompraResponse;
import com.fablab.financeiro.service.SolicitacaoCompraService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Solicitações de compra (fluxo informativo para o Estoque &amp; Suprimentos).
 */
@RestController
@RequestMapping("/solicitacoes-compra")
@PreAuthorize("hasRole('ADMIN')")
public class SolicitacaoCompraController {

    private final SolicitacaoCompraService solicitacaoService;

    public SolicitacaoCompraController(SolicitacaoCompraService solicitacaoService) {
        this.solicitacaoService = solicitacaoService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public SolicitacaoCompraResponse criar(@Valid @RequestBody SolicitacaoCompraRequest request) {
        return solicitacaoService.criar(request);
    }

    @PutMapping("/{idSolicitacao}/concluir")
    public SolicitacaoCompraResponse concluir(@PathVariable Long idSolicitacao) {
        return solicitacaoService.concluir(idSolicitacao);
    }
}