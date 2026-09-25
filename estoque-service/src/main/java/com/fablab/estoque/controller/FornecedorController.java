package com.fablab.estoque.controller;

import com.fablab.estoque.dto.FornecedorRequest;
import com.fablab.estoque.dto.FornecedorResponse;
import com.fablab.estoque.service.FornecedorService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Endpoints de fornecedores.
 */
@RestController
@RequestMapping("/fornecedores")
public class FornecedorController {

    private static final String EDICAO =
            "hasAnyRole('ADMIN','BOLSISTA')";

    private final FornecedorService fornecedorService;

    public FornecedorController(FornecedorService fornecedorService) {
        this.fornecedorService = fornecedorService;
    }

    @PostMapping
    @PreAuthorize(EDICAO)
    public ResponseEntity<FornecedorResponse> criar(@Valid @RequestBody FornecedorRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(fornecedorService.criar(request));
    }

    @GetMapping
    @PreAuthorize(EDICAO)
    public ResponseEntity<List<FornecedorResponse>> listar() {
        return ResponseEntity.ok(fornecedorService.listar());
    }
}