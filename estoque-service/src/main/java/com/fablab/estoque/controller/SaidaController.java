package com.fablab.estoque.controller;

import com.fablab.estoque.dto.SaidaRequest;
import com.fablab.estoque.dto.SaidaResponse;
import com.fablab.estoque.service.SaidaService;
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
 * Endpoints de saídas manuais de estoque (consumo, perda, ajuste).
 */
@RestController
@RequestMapping("/saidas")
public class SaidaController {

    private static final String VISUALIZACAO =
            "hasAnyRole('ADMIN','BOLSISTA','VOLUNTARIO','ESTAGIARIO')";

    private static final String EDICAO =
            "hasAnyRole('ADMIN','BOLSISTA')";

    private final SaidaService saidaService;

    public SaidaController(SaidaService saidaService) {
        this.saidaService = saidaService;
    }

    @PostMapping
    @PreAuthorize(EDICAO)
    public ResponseEntity<SaidaResponse> registrar(@Valid @RequestBody SaidaRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(saidaService.registrar(request));
    }

    /**
     * Lista global de saídas (E-1/R-9). Sem {@code idItem} retorna todas;
     * com {@code idItem} restringe ao histórico do item.
     */
    @GetMapping
    @PreAuthorize(VISUALIZACAO)
    public ResponseEntity<List<SaidaResponse>> listar(
            @RequestParam(required = false) Long idItem) {
        return ResponseEntity.ok(saidaService.listar(idItem));
    }

    /** Detalhe de uma saída (tela {@code /saidas/[id]}). */
    @GetMapping("/{id}")
    @PreAuthorize(VISUALIZACAO)
    public ResponseEntity<SaidaResponse> buscar(@PathVariable Long id) {
        return ResponseEntity.ok(saidaService.buscar(id));
    }
}