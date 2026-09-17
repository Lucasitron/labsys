package com.fablab.financeiro.controller;

import com.fablab.financeiro.dto.FechamentoEncomendaRequest;
import com.fablab.financeiro.dto.FechamentoEncomendaResponse;
import com.fablab.financeiro.service.FechamentoEncomendaService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Fechamento de encomendas (valores congelados após a criação).
 */
@RestController
@RequestMapping("/fechamento-encomenda")
@PreAuthorize("hasRole('ADMIN')")
public class FechamentoEncomendaController {

    private final FechamentoEncomendaService fechamentoService;

    public FechamentoEncomendaController(FechamentoEncomendaService fechamentoService) {
        this.fechamentoService = fechamentoService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public FechamentoEncomendaResponse criar(@Valid @RequestBody FechamentoEncomendaRequest request) {
        return fechamentoService.criar(request);
    }

    @GetMapping("/{idEncomenda}")
    public FechamentoEncomendaResponse obterPorEncomenda(@PathVariable Long idEncomenda) {
        return fechamentoService.obterPorEncomenda(idEncomenda);
    }
}