package com.fablab.financeiro.controller;

import com.fablab.financeiro.dto.DoacaoDtos.DoacaoRequest;
import com.fablab.financeiro.dto.DoacaoDtos.DoacaoResponse;
import com.fablab.financeiro.entity.TipoDoacao;
import com.fablab.financeiro.service.DoacaoRecursoService;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Endpoints de doações e recursos de projetos. */
@RestController
@RequestMapping("/doacoes-recursos")
@PreAuthorize("hasRole('ADMIN')")
public class DoacaoController {

    private final DoacaoRecursoService service;

    public DoacaoController(DoacaoRecursoService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<DoacaoResponse> registrar(@Valid @RequestBody DoacaoRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.registrar(request));
    }

    @GetMapping
    public ResponseEntity<List<DoacaoResponse>> listar(
            @RequestParam(required = false) TipoDoacao tipo,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataInicio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataFim) {
        return ResponseEntity.ok(service.listar(tipo, dataInicio, dataFim));
    }
}
