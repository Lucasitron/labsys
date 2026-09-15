package com.fablab.vendas.controller;

import com.fablab.vendas.dto.InteracaoRequest;
import com.fablab.vendas.dto.InteracaoResponse;
import com.fablab.vendas.service.InteracaoService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Endpoints de interações com clientes (CRM interno).
 */
@RestController
@RequestMapping("/interacoes")
public class InteracaoController {

    private static final String VISUALIZACAO =
            "hasAnyRole('ADMIN','BOLSISTA','VOLUNTARIO','ESTAGIARIO')";

    private static final String ESCRITA =
            "hasAnyRole('ADMIN','BOLSISTA','VOLUNTARIO')";

    private final InteracaoService interacaoService;

    public InteracaoController(InteracaoService interacaoService) {
        this.interacaoService = interacaoService;
    }

    @PostMapping
    @PreAuthorize(ESCRITA)
    public ResponseEntity<InteracaoResponse> registrar(@Valid @RequestBody InteracaoRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(interacaoService.registrar(request));
    }

    @GetMapping("/{idCliente}")
    @PreAuthorize(VISUALIZACAO)
    public ResponseEntity<List<InteracaoResponse>> listarPorCliente(@PathVariable Long idCliente) {
        return ResponseEntity.ok(interacaoService.listarPorCliente(idCliente));
    }
}