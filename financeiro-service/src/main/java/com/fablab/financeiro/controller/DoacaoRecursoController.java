package com.fablab.financeiro.controller;

import com.fablab.financeiro.dto.DoacaoRecursoRequest;
import com.fablab.financeiro.dto.DoacaoRecursoResponse;
import com.fablab.financeiro.entity.TipoDoacaoRecurso;
import com.fablab.financeiro.service.DoacaoRecursoService;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Doações e recursos de projetos universitários.
 */
@RestController
@RequestMapping("/doacoes-recursos")
@PreAuthorize("hasRole('ADMIN')")
public class DoacaoRecursoController {

    private final DoacaoRecursoService doacaoRecursoService;

    public DoacaoRecursoController(DoacaoRecursoService doacaoRecursoService) {
        this.doacaoRecursoService = doacaoRecursoService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public DoacaoRecursoResponse criar(@Valid @RequestBody DoacaoRecursoRequest request) {
        return doacaoRecursoService.criar(request);
    }

    @GetMapping
    public List<DoacaoRecursoResponse> listar(
            @RequestParam(required = false) TipoDoacaoRecurso tipo,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataInicio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataFim) {
        return doacaoRecursoService.listar(tipo, dataInicio, dataFim);
    }
}