package com.fablab.estoque.controller;

import com.fablab.estoque.dto.BomConsumoRequest;
import com.fablab.estoque.dto.BomRequest;
import com.fablab.estoque.dto.BomResponse;
import com.fablab.estoque.service.BomService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Endpoints de Listas de Materiais (BOM).
 */
@RestController
@RequestMapping("/boms")
public class BomController {

    public static final String VISUALIZACAO =
            "hasAnyRole('ADMIN','BOLSISTA','VOLUNTARIO','ESTAGIARIO')";

    private static final String EDICAO =
            "hasAnyRole('ADMIN','BOLSISTA')";

    private final BomService bomService;

    public BomController(BomService bomService) {
        this.bomService = bomService;
    }

    @PostMapping
    @PreAuthorize(EDICAO)
    public ResponseEntity<BomResponse> criar(@Valid @RequestBody BomRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(bomService.criar(request));
    }

    @GetMapping("/{id}")
    @PreAuthorize(VISUALIZACAO)
    public ResponseEntity<BomResponse> buscar(@PathVariable Long id) {
        return ResponseEntity.ok(bomService.buscar(id));
    }

    @PutMapping("/{id}")
    @PreAuthorize(EDICAO)
    public ResponseEntity<BomResponse> atualizar(@PathVariable Long id,
                                                 @Valid @RequestBody BomRequest request) {
        return ResponseEntity.ok(bomService.atualizar(id, request));
    }

    @PostMapping("/{id}/consumo")
    @PreAuthorize(EDICAO)
    public ResponseEntity<BomResponse> registrarConsumo(@PathVariable Long id,
                                                        @Valid @RequestBody BomConsumoRequest request) {
        return ResponseEntity.ok(bomService.registrarConsumo(id, request));
    }
}