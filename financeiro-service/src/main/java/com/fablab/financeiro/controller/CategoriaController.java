package com.fablab.financeiro.controller;

import com.fablab.financeiro.dto.CategoriaDtos.CategoriaRequest;
import com.fablab.financeiro.dto.CategoriaDtos.CategoriaResponse;
import com.fablab.financeiro.entity.TipoCategoria;
import com.fablab.financeiro.service.CategoriaFinanceiraService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Endpoints de categorias financeiras. */
@RestController
@RequestMapping("/categorias")
@PreAuthorize("hasRole('ADMIN')")
public class CategoriaController {

    private final CategoriaFinanceiraService service;

    public CategoriaController(CategoriaFinanceiraService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<CategoriaResponse> criar(@Valid @RequestBody CategoriaRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.criar(request));
    }

    @GetMapping
    public ResponseEntity<List<CategoriaResponse>> listar(@RequestParam(required = false) TipoCategoria tipo) {
        return ResponseEntity.ok(service.listar(tipo));
    }
}
