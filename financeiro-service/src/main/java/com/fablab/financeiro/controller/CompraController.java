package com.fablab.financeiro.controller;

import com.fablab.financeiro.dto.CompraDtos.CompraRequest;
import com.fablab.financeiro.dto.CompraDtos.CompraResponse;
import com.fablab.financeiro.entity.StatusCompra;
import com.fablab.financeiro.service.SolicitacaoCompraService;
import jakarta.validation.Valid;
import java.util.List;
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

/** Endpoints de solicitações de compra (fluxo informativo). */
@RestController
@RequestMapping("/solicitacoes-compra")
@PreAuthorize("hasRole('ADMIN')")
public class CompraController {

    private final SolicitacaoCompraService service;

    public CompraController(SolicitacaoCompraService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<CompraResponse> registrar(@Valid @RequestBody CompraRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.registrar(request));
    }

    @GetMapping
    public ResponseEntity<List<CompraResponse>> listar(@RequestParam(required = false) StatusCompra status) {
        return ResponseEntity.ok(service.listar(status));
    }

    @GetMapping("/{id}")
    public ResponseEntity<CompraResponse> detalhar(@PathVariable Long id) {
        return ResponseEntity.ok(service.detalhar(id));
    }

    @PutMapping("/{id}/concluir")
    public ResponseEntity<CompraResponse> concluir(@PathVariable Long id) {
        return ResponseEntity.ok(service.concluir(id));
    }
}
