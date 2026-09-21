package com.fablab.estoque.controller;

import com.fablab.estoque.dto.EntradaRequest;
import com.fablab.estoque.dto.EntradaResponse;
import com.fablab.estoque.service.EntradaService;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Endpoints de entradas de estoque (compras simples).
 */
@RestController
@RequestMapping("/entradas")
public class EntradaController {

    private static final String VISUALIZACAO =
            "hasAnyRole('ADMIN','BOLSISTA','VOLUNTARIO','ESTAGIARIO')";

    private static final String EDICAO =
            "hasAnyRole('ADMIN','BOLSISTA')";

    private final EntradaService entradaService;

    public EntradaController(EntradaService entradaService) {
        this.entradaService = entradaService;
    }

    @PostMapping
    @PreAuthorize(EDICAO)
    public ResponseEntity<EntradaResponse> registrar(@Valid @RequestBody EntradaRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(entradaService.registrar(request));
    }

    /** Histórico de entradas de um item. */
    @GetMapping
    @PreAuthorize(VISUALIZACAO)
    public ResponseEntity<List<EntradaResponse>> listarPorItem(@RequestParam Long idItem) {
        return ResponseEntity.ok(entradaService.listarPorItem(idItem));
    }

    /** Detalhe de uma entrada (tela {@code /entradas/[id]}). */
    @GetMapping("/{id}")
    @PreAuthorize(VISUALIZACAO)
    public ResponseEntity<EntradaResponse> buscar(@PathVariable Long id) {
        return ResponseEntity.ok(entradaService.buscar(id));
    }
}